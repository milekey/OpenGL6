package com.scaredeer.opengl;

import static android.opengl.GLES20.GL_FLOAT;
import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TRIANGLE_STRIP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDrawArrays;
import static android.opengl.GLES20.glEnableVertexAttribArray;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glVertexAttribPointer;

import android.annotation.SuppressLint;

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

    private final int mTextureName;

    /**
     * @param shaderProgram ShaderProgram オブジェクト。
     * @param left          タイルの左辺の座標
     * @param top           タイルの上辺の座標
     * @param textureName   テクスチャーに使う Bitmap オブジェクト
     */
    @SuppressLint("DefaultLocale")
    public Tile(ShaderProgram shaderProgram, int left, int top, int textureName) {
        mShaderProgram = shaderProgram;

        float[] square = buildSquare(left, top);
        mVertexData = ByteBuffer
                .allocateDirect(square.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(square);

        mTextureName = textureName;
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
        glBindTexture(GL_TEXTURE_2D, mTextureName);

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
        if (mTextureName != 0) { // mTexture が無効でない（0 でない）場合のみ実行
            setTexture();
            bindDataToShaderVariable();
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);
        }
    }
}