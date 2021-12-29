package com.scaredeer.opengl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20.*
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * ゲームのメインループに相当するクラス
 * （もちろん、画面の更新を中心としたもので、ゲームモデルの論理的なループとは必ずしも同じではないが、
 * 実用上はこのクラスを中心に構成していいと思う）
 *
 * MainActivity で implement して記述を統合することも全然可能だが、
 * MainActivity はその他の UI のコードなども盛り込まれることになるので、コードの見通しが悪くなり、
 * あまり実用的ではないので、素直に分離している。
 */
class Renderer(context: Context?) : GLSurfaceView.Renderer {

    companion object {
        private val TAG = Renderer::class.simpleName
    }

    private val mBitmap: Bitmap? = Texture.loadBitmap(context!!, R.drawable.hill_14_14552_6451)
    private var mTexture: Texture? = null
    private var mTile: Tile? = null

    private val mBitmapOverlay: Bitmap? = Texture.loadBitmap(context!!, R.drawable.pale_14_14552_6451)
    private var mTextureOverlay: Texture? = null
    private var mTileOverlay: Tile? = null

    private var mShaderProgram: ShaderProgram? = null
    private var mShaderProgramOverlay: ShaderProgram? = null

    private val mProjectionMatrix = FloatArray(16)
    private val mViewMatrix = FloatArray(16)
    private val mVpMatrix = FloatArray(16)

    override fun onSurfaceCreated(gl10: GL10, eglConfig: EGLConfig) {
        Log.v(TAG, "onSurfaceCreated")
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        mShaderProgram = ShaderProgram(1.0f)
        mShaderProgramOverlay = ShaderProgram(0.6f)

        mTexture = Texture(mBitmap!!)
        mBitmap.recycle()
        mTextureOverlay = Texture(mBitmapOverlay!!)
        mBitmapOverlay.recycle()
    }

    override fun onSurfaceChanged(gl10: GL10, width: Int, height: Int) {
        Log.v(TAG, "onSurfaceChanged")
        // Set the OpenGL viewport to fill the entire surface.
        glViewport(0, 0, width, height)

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
        Matrix.frustumM(
            mProjectionMatrix, 0,
            -width / 4f, width / 4f, -height / 4f, height / 4f,
            width / 4f, width / 2f
        )

        /*
        UV 座標系と同じに扱うには、右手系のまま、単に、カメラの上下を引っくり返せば、Y 軸が下向きになるばかりでなく、
        Z 軸も奥に向って正方向となり、色々と感覚的にシームレスになる。これが upY = -1 の理由である。
        また、centerX/centerY は視線を常に z 軸に平行とするため、eyeX/eyeY と共通にする。
        eyeZ については本来は任意なのだが、わかりやすいので画面横幅を斜辺とする直角二等辺三角形になるようにする値を
        選ぶ（つまり width/2 の距離）。かつ z = 0 が、斜辺の位置とする（つまり z 座標は -width/2）。
        eyeZ = -eyeX となっているわけである。
         */
        Matrix.setLookAtM(
            mViewMatrix, 0,
            width / 2f, height / 2f, -width / 2f,
            width / 2f, height / 2f, 1f,
            0f, -1f, 0f
        )
        Matrix.multiplyMM(
            mVpMatrix, 0,
            mProjectionMatrix, 0, mViewMatrix, 0
        )

        // setMVPMatrix（glUniformMatrix4fv）するにあたってあらかじめ use（glUseProgram）する必要がある
        mShaderProgram!!.use()
        // Pass the matrix into the shader program.
        mShaderProgram!!.setMVPMatrix(mVpMatrix)
        mTile = Tile(mShaderProgram!!, 0, 0, mTexture!!.name)

        // setMVPMatrix（glUniformMatrix4fv）するにあたってあらかじめ use（glUseProgram）する必要がある
        mShaderProgramOverlay!!.use()
        // Pass the matrix into the shader program.
        mShaderProgramOverlay!!.setMVPMatrix(mVpMatrix)
        mTileOverlay = Tile(mShaderProgramOverlay!!, 0, 0, mTextureOverlay!!.name)
    }

    override fun onDrawFrame(gl10: GL10) {
        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT)

        mShaderProgram!!.use()
        mTile!!.draw()

        mShaderProgramOverlay!!.use()
        mTileOverlay!!.draw()
    }
}