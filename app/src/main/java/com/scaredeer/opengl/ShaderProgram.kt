package com.scaredeer.opengl

import android.opengl.GLES20
import android.util.Log

class ShaderProgram(alpha: Float) {

    companion object {
        private val TAG = ShaderProgram::class.simpleName

        private const val U_MVP_MATRIX = "u_MVPMatrix"
        private const val A_POSITION = "a_Position"
        private const val A_TEXTURE_COORDINATES = "a_TextureCoordinates"
        private const val V_TEXTURE_COORDINATES = "v_TextureCoordinates"
        private const val U_TEXTURE_UNIT = "u_TextureUnit"

        private const val VERTEX_SHADER = """
            uniform mat4 $U_MVP_MATRIX;
            attribute vec4 $A_POSITION;
            attribute vec2 $A_TEXTURE_COORDINATES;
            varying vec2 $V_TEXTURE_COORDINATES;
            void main() {
                gl_Position = $U_MVP_MATRIX * $A_POSITION;
                $V_TEXTURE_COORDINATES = $A_TEXTURE_COORDINATES;
            }
        """

        // --------------- ShaderHelpers -----------------------------------------------------------

        /**
         * Compiles a shader, returning the OpenGL object ID.
         *
         * @see <a href="https://media.pragprog.com/titles/kbogla/code/AirHockey1/src/com/airhockey/android/util/ShaderHelper.java">OpenGL ES 2 for Android</a>
         * @param type       GL_VERTEX_SHADER or GL_FRAGMENT_SHADER
         * @param shaderCode String data of shader code
         * @return the OpenGL object ID (or 0 if compilation failed)
         */
        private fun compileShader(type: Int, shaderCode: String): Int {
            // Create a new shader object.
            val shaderObjectId = GLES20.glCreateShader(type)
            if (shaderObjectId == 0) {
                Log.w(TAG, "Could not create new shader.")
                return 0
            }

            // Pass in (upload) the shader source.
            GLES20.glShaderSource(shaderObjectId, shaderCode)

            // Compile the shader.
            GLES20.glCompileShader(shaderObjectId)

            // Get the compilation status.
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shaderObjectId, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

            // Print the shader info log to the Android log output.
            Log.v(TAG, """
            Result of compiling source:
                $shaderCode
            Log:
                ${GLES20.glGetShaderInfoLog(shaderObjectId)}
            """.trimIndent())

            // Verify the compile status.
            if (compileStatus[0] == 0) {
                // If it failed, delete the shader object.
                GLES20.glDeleteShader(shaderObjectId)
                Log.w(TAG, "Compilation of shader failed.")
                return 0
            }

            // Return the shader object ID.
            return shaderObjectId
        }

        /**
         * Links a vertex shader and a fragment shader together into an OpenGL
         * program. Returns the OpenGL program object ID, or 0 if linking failed.
         *
         * @see <a href="https://media.pragprog.com/titles/kbogla/code/AirHockey1/src/com/airhockey/android/util/ShaderHelper.java">OpenGL ES 2 for Android</a>
         * @param vertexShaderId   OpenGL object ID of vertex shader
         * @param fragmentShaderId OpenGL object ID of fragment shader
         * @return OpenGL program object ID (or 0 if linking failed)
         */
        private fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
            // Create a new program object.
            val programObjectId = GLES20.glCreateProgram()
            if (programObjectId == 0) {
                Log.w(TAG, "Could not create new program")
                return 0
            }

            // Attach the vertex shader to the program.
            GLES20.glAttachShader(programObjectId, vertexShaderId)
            // Attach the fragment shader to the program.
            GLES20.glAttachShader(programObjectId, fragmentShaderId)

            // Link the two shaders together into a program.
            GLES20.glLinkProgram(programObjectId)

            // Get the link status.
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(programObjectId, GLES20.GL_LINK_STATUS, linkStatus, 0)

            // Print the program info log to the Android log output.
            Log.v(TAG, """
                Result log of linking program:
                ${GLES20.glGetProgramInfoLog(programObjectId)}
            """.trimIndent())

            // Verify the link status.
            if (linkStatus[0] == 0) {
                // If it failed, delete the program object.
                GLES20.glDeleteProgram(programObjectId)
                Log.w(TAG, "Linking of program failed.")
                return 0
            }

            // Return the program object ID.
            return programObjectId
        }

        /**
         * Validates an OpenGL program. Should only be called when developing the application.
         *
         * @see <a href="https://media.pragprog.com/titles/kbogla/code/AirHockey1/src/com/airhockey/android/util/ShaderHelper.java">OpenGL ES 2 for Android</a>
         * @param programObjectId OpenGL program object ID to validate
         * @return boolean
         */
        private fun validateProgram(programObjectId: Int): Boolean {
            GLES20.glValidateProgram(programObjectId)
            val validateStatus = IntArray(1)
            GLES20.glGetProgramiv(programObjectId, GLES20.GL_VALIDATE_STATUS, validateStatus, 0)
            Log.v(TAG, """
                Result status code of validating program: ${validateStatus[0]}
                Log:
                ${GLES20.glGetProgramInfoLog(programObjectId)}
            """.trimIndent())
            return validateStatus[0] != 0
        }
    }

    private val mShaderProgram: Int
    private val mUMvpMatrix: Int

    val aPosition: Int
    val aTextureCoordinates: Int
    val uTextureUnit: Int

    init {
        // https://codehero.jp/opengl/18783752/fragment-shader-and-coloring-a-texture
        val fragmentShaderSource = """
            precision mediump float;
            uniform sampler2D $U_TEXTURE_UNIT;
            varying vec2 $V_TEXTURE_COORDINATES;
            void main() {
                vec4 tex = texture2D($U_TEXTURE_UNIT, $V_TEXTURE_COORDINATES);
                gl_FragColor = vec4(tex.r, tex.g, tex.b, $alpha);
            }
        """

        // Compile the shaders.
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource)

        // Link them into a shader program.
        mShaderProgram = linkProgram(vertexShader, fragmentShader)
        validateProgram(mShaderProgram)

        // Retrieve pointer indices for input variables.
        mUMvpMatrix = GLES20.glGetUniformLocation(mShaderProgram, U_MVP_MATRIX)
        aPosition = GLES20.glGetAttribLocation(mShaderProgram, A_POSITION)
        aTextureCoordinates = GLES20.glGetAttribLocation(mShaderProgram, A_TEXTURE_COORDINATES)
        uTextureUnit = GLES20.glGetUniformLocation(mShaderProgram, U_TEXTURE_UNIT)
    }

    fun use() {
        // Set the current OpenGL shader program to this program.
        GLES20.glUseProgram(mShaderProgram)
    }

    /**
     * バーテックスシェーダーで定義している MVPMatrix をセットし直すためのメソッド。
     *
     * @param mvpMatrix MVPMatrix
     */
    fun setMVPMatrix(mvpMatrix: FloatArray?) {
        // Pass the matrix into the shader program.
        GLES20.glUniformMatrix4fv(mUMvpMatrix, 1, false, mvpMatrix, 0)
    }
}