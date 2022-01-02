package com.scaredeer.opengl

import android.opengl.GLES20.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * @param [shaderProgram] ShaderProgram オブジェクト。
 * @param [left]          タイルの左辺の座標
 * @param [top]           タイルの上辺の座標
 * @param [textureName]   テクスチャーに使う Texture オブジェクト名（as int）
 */
class Tile(
    private val shaderProgram: ShaderProgram,
    left: Int, top: Int,
    private val textureName: Int,
) {

    companion object {
        private val TAG = Tile::class.simpleName

        private const val BYTES_PER_FLOAT = 4 // Java float is 32-bit = 4-byte
        private const val POSITION_COMPONENT_COUNT = 2 // x, y（※ z は常に 0 なので省略）
        private const val TEXTURE_COORDINATES_COMPONENT_COUNT = 2 // s, t

        // STRIDE は要するに、次の頂点データセットへスキップするバイト数を表したもの。
        // 一つの頂点データセットは頂点座標 x, y とテクスチャー座標の s, t の 4 つで構成されているが、
        // 頂点データを読み取ってシェーダー変数へ渡す処理と、テクスチャー座標を読み取ってシェーダー変数に渡す処理が
        // 別々であるため、次の頂点を処理する際に、4 つ分のバイト数をスキップする必要が生じる。
        private const val STRIDE =
            (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT

        /**
         * モデル座標系もテクスチャー座標系も共に、UV 座標系（Y 軸が下向き）を使うことにする。
         *
         * ちなみに、Z 成分は常に 0 しか使わないので、モデルにとってはどうでもいいことだが、
         * OpenGL 標準の右手座標系のまま、X 軸を軸にして Z 軸が奥行方向に向くように回転すれば、Y 軸が下に向くので、
         * カメラの指定（setLookAtM）を上下逆さま（upY = -1）にすれば上手く辻褄を合わせられる。
         *
         * @param left タイル左上角の位置を UV 座標系（Y 軸が下向き、画面左上角に原点）で指定する。
         * @param top  タイル左上角の位置 UV 座標系（Y 軸が下向き、画面左上角に原点）で指定する。
         * @return float[]
         */
        private fun buildSquare(left: Int, top: Int): FloatArray {
            return floatArrayOf(
                // x, y, s, t
                left.toFloat(), top.toFloat(), 0f, 0f, // 左上
                left.toFloat(), top + 256f, 0f, 1f,    // 左下
                left + 256f, top.toFloat(), 1f, 0f,    // 右上
                left + 256f, top + 256f, 1f, 1f        // 右下
            )
        }
    }

    private val vertexData: FloatBuffer

    init {
        val square = buildSquare(left, top)
        vertexData = ByteBuffer
            .allocateDirect(square.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(square)
    }

    /**
     * draw で実行 (1/2)
     */
    private fun setTexture() {
        // Set the active texture unit to texture unit 0.
        glActiveTexture(GL_TEXTURE0)

        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, textureName)

        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0.
        glUniform1i(shaderProgram.uTextureUnit, 0)
    }

    /**
     * draw で実行 (2/2)
     */
    private fun bindDataToShaderVariable() {
        // Bind our data, specified by the variable vertexData, to the vertex
        // attribute at location A_POSITION.
        vertexData.position(0)
        glVertexAttribPointer(
            shaderProgram.aPosition,
            POSITION_COMPONENT_COUNT,
            GL_FLOAT,
            false,
            STRIDE,
            vertexData
        )
        glEnableVertexAttribArray(shaderProgram.aPosition)

        // Bind our data, specified by the variable vertexData, to the vertex
        // attribute at location A_TEXTURE_COORDINATES.
        vertexData.position(POSITION_COMPONENT_COUNT) // starts right after the vertex (x, y) coordinates
        glVertexAttribPointer(
            shaderProgram.aTextureCoordinates,
            TEXTURE_COORDINATES_COMPONENT_COUNT,
            GL_FLOAT,
            false,
            STRIDE,
            vertexData
        )
        glEnableVertexAttribArray(shaderProgram.aTextureCoordinates)
    }

    /**
     * GLSurfaceView.onDrawFrame でループ処理されるべき処理
     */
    fun draw() {
        if (textureName != 0) { // Texture が無効でない（0 でない）場合のみ実行
            setTexture()
            bindDataToShaderVariable()
            glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
        }
    }
}