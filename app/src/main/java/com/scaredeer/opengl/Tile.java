package com.scaredeer.opengl;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glVertexAttribPointer;
import static android.opengl.GLUtils.texImage2D;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Tile {
    private static final String TAG = "Tile";

    private final ShaderProgram mShaderProgram;

    private static final int BYTES_PER_FLOAT = 4; // Java float is 32-bit = 4-byte
    private static final int POSITION_COMPONENT_COUNT = 2; // x, y（※ z は常に 0 なので省略）
    private static final int TEXTURE_COORDINATES_COMPONENT_COUNT = 2; // s, t
    // STRIDE は要するに、次の頂点データセットへスキップするバイト数を表したもの。
    // 一つの頂点データセットは頂点座標 x, y とテクスチャー座標の s, t の 4 つで構成されているが、
    // 頂点データを読み取ってシェーダー変数へ渡す処理と、テクスチャー座標を読み取ってシェーダー変数に渡す処理が
    // 別々であるため、次の頂点を処理する際に、4 つ分のバイト数をスキップする必要が生じる。
    private static final int STRIDE
            = (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT;
    private final FloatBuffer mVertexData;

    private int[] mTextureObjectIds;
    private final int mTexture;

    /**
     * @param shaderProgram ShaderProgram オブジェクト。
     * @param left          タイルの左辺の座標
     * @param top           タイルの上辺の座標
     * @param bitmap        テクスチャーに使う Bitmap オブジェクト
     */
    @SuppressLint("DefaultLocale")
    public Tile(ShaderProgram shaderProgram, int left, int top, Bitmap bitmap) {
        mShaderProgram = shaderProgram;

        float[] square = buildSquare(left, top);
        mVertexData = ByteBuffer
                .allocateDirect(square.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(square);

        mTexture = loadTextureFromBitmap(bitmap);
    }

    /**
     * モデル座標系もテクスチャー座標系も共に、UV 座標系（Y 軸が下向き）を使うことにする。
     * <p>
     * ちなみに、Z 成分は常に 0 しか使わないので、モデルにとってはどうでもいいことだが、
     * OpenGL 標準の右手座標系のまま、X 軸を軸にして Z 軸が奥行方向に向くように回転すれば、Y 軸が下に向くので、
     * カメラの指定（setLookAtM）を上下逆さま（upY = -1）にすれば上手く辻褄を合わせられる。
     *
     * @param left タイル左上角の位置を UV 座標系（Y 軸が下向き、画面左上角に原点）で指定する。
     * @param top  タイル左上角の位置 UV 座標系（Y 軸が下向き、画面左上角に原点）で指定する。
     * @return float[]
     */
    private static float[] buildSquare(int left, int top) {
        return new float[]{ // x, y, s, t
                left, top, 0, 0, // 左上
                left, top + 256f, 0, 1f, // 左下
                left + 256f, top, 1f, 0, // 右上
                left + 256f, top + 256f, 1f, 1f // 右下
        };
    }

    /**
     * draw で実行 (1/2)
     */
    private void setTexture() {
        // Set the active texture unit to texture unit 0.
        glActiveTexture(GL_TEXTURE0);

        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, mTexture);

        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0.
        glUniform1i(mShaderProgram.uTextureUnit, 0);
    }

    /**
     * draw で実行 (2/2)
     */
    private void bindDataToShaderVariable() {
        // Bind our data, specified by the variable vertexData, to the vertex
        // attribute at location A_POSITION.
        mVertexData.position(0);
        glVertexAttribPointer(
                mShaderProgram.aPosition,
                POSITION_COMPONENT_COUNT,
                GL_FLOAT,
                false,
                STRIDE,
                mVertexData
        );
        glEnableVertexAttribArray(mShaderProgram.aPosition);

        // Bind our data, specified by the variable vertexData, to the vertex
        // attribute at location A_TEXTURE_COORDINATES.
        mVertexData.position(POSITION_COMPONENT_COUNT); // starts right after the vertex (x, y) coordinates
        glVertexAttribPointer(
                mShaderProgram.aTextureCoordinates,
                TEXTURE_COORDINATES_COMPONENT_COUNT,
                GL_FLOAT,
                false,
                STRIDE,
                mVertexData
        );
        glEnableVertexAttribArray(mShaderProgram.aTextureCoordinates);
    }

    /**
     * GLSurfaceView.onDrawFrame でループ処理されるべき処理
     */
    public void draw() {
        if (mTexture != 0) { // mTexture が無効でない（0 でない）場合のみ実行
            setTexture();
            bindDataToShaderVariable();
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        }
    }

    /**
     * @param bitmap Loads a texture from a Bitmap
     * @return OpenGL ID for the texture. Returns 0 if the load failed.
     */
    private int loadTextureFromBitmap(Bitmap bitmap) {
        mTextureObjectIds = new int[1];
        glGenTextures(1, mTextureObjectIds, 0);
        if (mTextureObjectIds[0] == 0) {
            Log.w(TAG, "Could not generate a new OpenGL texture object.");
            return 0;
        }

        // Bind to the texture in OpenGL
        glBindTexture(GL_TEXTURE_2D, mTextureObjectIds[0]);

        // Set filtering: a default must be set, or the texture will be black.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        // Load the bitmap into the bound texture.
        texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

        // Note: Following code may cause an error to be reported in the
        // ADB log as follows: E/IMGSRV(20095): :0: HardwareMipGen:
        // Failed to generate texture mipmap levels (error=3)
        // No OpenGL error will be encountered (glGetError() will return
        // 0). If this happens, just squash the source image to be
        // square. It will look the same because of texture coordinates,
        // and mipmap generation will work.

        glGenerateMipmap(GL_TEXTURE_2D);

        // Recycle the bitmap, since its data has been loaded into
        // OpenGL.
        bitmap.recycle();

        // Unbind from the texture.
        glBindTexture(GL_TEXTURE_2D, 0);

        return mTextureObjectIds[0];
    }

    /**
     * リソースから Bitmap を読み込みたい場合に。loadTextureFromUrl の場合は不要。
     *
     * @param context    Context オブジェクト
     * @param resourceId R.drawable.XXX
     * @return Bitmap オブジェクト
     */
    public static Bitmap loadBitmap(Context context, int resourceId) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        // Read in the resource
        final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

        if (bitmap == null) {
            Log.w(TAG, "Resource ID " + resourceId + " could not be decoded.");
            return null;
        }

        return bitmap;
    }

    public void deleteTexture() {
        if (mTextureObjectIds != null) {
            glDeleteTextures(1, mTextureObjectIds, 0);
        }
    }
}