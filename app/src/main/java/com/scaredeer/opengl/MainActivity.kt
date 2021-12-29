package com.scaredeer.opengl;

import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.GL_SRC_ALPHA;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glEnable;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.frustumM;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.setLookAtM;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends AppCompatActivity implements GLSurfaceView.Renderer {
    private static final String TAG = "MainActivity";

    private GLSurfaceView mGLSurfaceView;

    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mVPMatrix = new float[16];

    private ShaderProgram mShaderProgram;
    private Bitmap mBitmap;
    private Texture mTexture;
    private Tile mTile;

    private ShaderProgram mShaderProgramOverlay;
    private Bitmap mBitmapOverlay;
    private Texture mTextureOverlay;
    private Tile mTileOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mGLSurfaceView.setRenderer(this);

        setContentView(mGLSurfaceView);

        mBitmap = Texture.loadBitmap(this, R.drawable.hill_14_14552_6451);
        mBitmapOverlay = Texture.loadBitmap(this, R.drawable.pale_14_14552_6451);
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG, "onDestroy");
        mGLSurfaceView.setRenderer(null);
        mGLSurfaceView = null;
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, "onPause");
        mGLSurfaceView.onPause();
        super.onPause();
    }

    // GLSurfaceView.Renderer (1/3)
    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        Log.v(TAG, "onSurfaceCreated");

        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        mShaderProgram = new ShaderProgram(1.0f);
        mShaderProgramOverlay = new ShaderProgram(0.6f);

        mTexture = new Texture(mBitmap);
        mTextureOverlay = new Texture(mBitmapOverlay);
    }

    // GLSurfaceView.Renderer (2/3)
    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        Log.v(TAG, "onSurfaceChanged");
        // Set the OpenGL viewport to fill the entire surface.
        glViewport(0, 0, width, height);

        /*
        frustum は lookAtM で決定される視軸に対して相対的な視界範囲を決めるという形で使われるものと思われ、
        （Web に見つかるのは perspective の用例ばかりであり、
        frustum についてはあくまでも自分で実地に色々と値を代入してテストしてみた結果による判断ではあるが）
        必ず、x 方向, y 方向は ± で指定する必要があるようである。（near/far にしても、
        絶対的な z 座標「位置」ではなくて、カメラ位置からの相対的な z 方向の「距離」を指定しているのと同じように。）

        特に near プレーンは単なるクリッピングエリアを決めるだけでなく、left/right/bottom/top と合わせて、
        視野角を決定する要因にもなるので、クリッピングエリアを変更するつもりで、気安くいじらないこと。

        far プレーンについては、等倍（ドット・バイ・ドット）の位置（z 座標は 0）を指定している。
        near プレーンはその半分の距離の位置（z 座標は -width/4）にあるとし、left/right/bottom/top を描画領域
        の 1/4 ずつの値にすることで、near プレーンにおいて縦横の幅が半分のサイズの長方形となるようにしている。

        こうすることで、far プレーンで等倍が実現できるので、そこに置くオブジェクトのサイズはそのまま画面の
        ピクセルサイズに基くことが可能となる。
         */
        frustumM(mProjectionMatrix, 0,
                -width / 4f, width / 4f, -height / 4f, height / 4f,
                width / 4f, width / 2f);

        /*
        UV 座標系と同じに扱うには、右手系のまま、単に、カメラの上下を引っくり返せば、Y 軸が下向きになるばかりでなく、
        Z 軸も奥に向って正方向となり、色々と感覚的にシームレスになる。これが upY = -1 の理由である。
        また、centerX/centerY は視線を常に z 軸に平行とするため、eyeX/eyeY と共通にする。
        eyeZ については本来は任意なのだが、わかりやすいので画面横幅を斜辺とする直角二等辺三角形になるようにする値を
        選ぶ（つまり width/2 の距離）。かつ z = 0 が、斜辺の位置とする（つまり z 座標は -width/2）。
        eyeZ = -eyeX となっているわけである。
         */
        setLookAtM(
                mViewMatrix, 0,
                width / 2f, height / 2f, -width / 2f,
                width / 2f, height / 2f, 1,
                0, -1, 0
        );

        multiplyMM(mVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        // setMVPMatrix（glUniformMatrix4fv）するにあたってあらかじめ use（glUseProgram）する必要がある
        mShaderProgram.use();
        // Pass the matrix into the shader program.
        mShaderProgram.setMVPMatrix(mVPMatrix);
        mTile = new Tile(mShaderProgram, 0, 0, mTexture.name);

        // setMVPMatrix（glUniformMatrix4fv）するにあたってあらかじめ use（glUseProgram）する必要がある
        mShaderProgramOverlay.use();
        // Pass the matrix into the shader program.
        mShaderProgramOverlay.setMVPMatrix(mVPMatrix);
        mTileOverlay = new Tile(mShaderProgramOverlay, 0, 0, mTextureOverlay.name);
    }

    // GLSurfaceView.Renderer (3/3)
    @Override
    public void onDrawFrame(GL10 gl10) {
        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT);

        mShaderProgram.use();
        mTile.draw();

        mShaderProgramOverlay.use();
        mTileOverlay.draw();
    }
}