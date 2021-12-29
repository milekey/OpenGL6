package com.scaredeer.opengl;

import static android.opengl.GLES20.GL_COMPILE_STATUS;
import static android.opengl.GLES20.GL_FRAGMENT_SHADER;
import static android.opengl.GLES20.GL_LINK_STATUS;
import static android.opengl.GLES20.GL_VALIDATE_STATUS;
import static android.opengl.GLES20.GL_VERTEX_SHADER;
import static android.opengl.GLES20.glAttachShader;
import static android.opengl.GLES20.glCompileShader;
import static android.opengl.GLES20.glCreateProgram;
import static android.opengl.GLES20.glCreateShader;
import static android.opengl.GLES20.glDeleteProgram;
import static android.opengl.GLES20.glDeleteShader;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetProgramInfoLog;
import static android.opengl.GLES20.glGetProgramiv;
import static android.opengl.GLES20.glGetShaderInfoLog;
import static android.opengl.GLES20.glGetShaderiv;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glLinkProgram;
import static android.opengl.GLES20.glShaderSource;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glValidateProgram;

import android.annotation.SuppressLint;
import android.util.Log;

public class ShaderProgram {
    private static final String TAG = "ShaderProgram";
    public final int shaderProgram;

    private static final String U_MVP_MATRIX = "u_MVPMatrix";
    public final int uMVPMatrix;
    private static final String A_POSITION = "a_Position";
    public final int aPosition;
    private static final String A_TEXTURE_COORDINATES = "a_TextureCoordinates";
    public final int aTextureCoordinates;
    private static final String V_TEXTURE_COORDINATES = "v_TextureCoordinates";
    private static final String U_TEXTURE_UNIT = "u_TextureUnit";
    public final int uTextureUnit;

    private static final String VERTEX_SHADER =
            String.format(
                    "uniform mat4 %s;\n" +
                            "attribute vec4 %s;\n" +
                            "attribute vec2 %s;\n" +
                            "varying vec2 %s;\n" +
                            "void main() {\n" +
                            "    gl_Position = %s * %s;\n" +
                            "    %s = %s;\n" +
                            "}",
                    U_MVP_MATRIX,
                    A_POSITION,
                    A_TEXTURE_COORDINATES,
                    V_TEXTURE_COORDINATES,
                    U_MVP_MATRIX, A_POSITION,
                    V_TEXTURE_COORDINATES, A_TEXTURE_COORDINATES
            );

    public ShaderProgram(float alpha) {
        // https://codehero.jp/opengl/18783752/fragment-shader-and-coloring-a-texture
        @SuppressLint("DefaultLocale") String fragmentShaderSource = String.format(
                "precision mediump float;\n" +
                        "uniform sampler2D %s;\n" +
                        "varying vec2 %s;\n" +
                        "void main() {\n" +
                        "    vec4 tex = texture2D(%s, %s);\n" +
                        "    gl_FragColor = vec4(tex.r, tex.g, tex.b, %f);\n" +
                        "}",
                U_TEXTURE_UNIT,
                V_TEXTURE_COORDINATES,
                U_TEXTURE_UNIT, V_TEXTURE_COORDINATES,
                alpha
        );

        // Compile the shaders.
        int vertexShader = compileShader(GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentShaderSource);

        // Link them into a shader program.
        shaderProgram = linkProgram(vertexShader, fragmentShader);
        validateProgram(shaderProgram);

        // Retrieve pointer indices for input variables.
        uMVPMatrix = glGetUniformLocation(shaderProgram, U_MVP_MATRIX);
        aPosition = glGetAttribLocation(shaderProgram, A_POSITION);
        aTextureCoordinates = glGetAttribLocation(shaderProgram, A_TEXTURE_COORDINATES);
        uTextureUnit = glGetUniformLocation(shaderProgram, U_TEXTURE_UNIT);
    }

    public void use() {
        // Set the current OpenGL shader program to this program.
        glUseProgram(shaderProgram);
    }

    /**
     * バーテックスシェーダーで定義している MVPMatrix をセットし直すためのメソッド。
     *
     * @param mvpMatrix MVPMatrix
     */
    public void setMVPMatrix(float[] mvpMatrix) {
        // Pass the matrix into the shader program.
        glUniformMatrix4fv(uMVPMatrix, 1, false, mvpMatrix, 0);
    }


    // --------------- ShaderHelper ---------------------------------------------------------

    /**
     * Compiles a shader, returning the OpenGL object ID.
     * <p>
     * cf. https://media.pragprog.com/titles/kbogla/code/AirHockey1/src/com/airhockey/android/util/ShaderHelper.java
     *
     * @param type       GL_VERTEX_SHADER or GL_FRAGMENT_SHADER
     * @param shaderCode String data of shader code
     * @return the OpenGL object ID (or 0 if compilation failed)
     */
    private static int compileShader(int type, String shaderCode) {
        // Create a new shader object.
        final int shaderObjectId = glCreateShader(type);
        if (shaderObjectId == 0) {
            Log.w(TAG, "Could not create new shader.");
            return 0;
        }

        // Pass in (upload) the shader source.
        glShaderSource(shaderObjectId, shaderCode);

        // Compile the shader.
        glCompileShader(shaderObjectId);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS, compileStatus, 0);

        // Print the shader info log to the Android log output.
        Log.v(TAG, String.format(
                "Results of compiling source:\n%s\n%s",
                shaderCode, glGetShaderInfoLog(shaderObjectId)
        ));

        // Verify the compile status.
        if (compileStatus[0] == 0) {
            // If it failed, delete the shader object.
            glDeleteShader(shaderObjectId);
            Log.w(TAG, "Compilation of shader failed.");
            return 0;
        }

        // Return the shader object ID.
        return shaderObjectId;
    }

    /**
     * Links a vertex shader and a fragment shader together into an OpenGL
     * program. Returns the OpenGL program object ID, or 0 if linking failed.
     * <p>
     * cf. https://media.pragprog.com/titles/kbogla/code/AirHockey1/src/com/airhockey/android/util/ShaderHelper.java
     *
     * @param vertexShaderId   OpenGL object ID of vertex shader
     * @param fragmentShaderId OpenGL object ID of fragment shader
     * @return OpenGL program object ID (or 0 if linking failed)
     */
    private static int linkProgram(int vertexShaderId, int fragmentShaderId) {
        // Create a new program object.
        final int programObjectId = glCreateProgram();
        if (programObjectId == 0) {
            Log.w(TAG, "Could not create new program");
            return 0;
        }

        // Attach the vertex shader to the program.
        glAttachShader(programObjectId, vertexShaderId);
        // Attach the fragment shader to the program.
        glAttachShader(programObjectId, fragmentShaderId);

        // Link the two shaders together into a program.
        glLinkProgram(programObjectId);

        // Get the link status.
        final int[] linkStatus = new int[1];
        glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0);

        // Print the program info log to the Android log output.
        Log.v(TAG, "Results of linking program:\n" + glGetProgramInfoLog(programObjectId));

        // Verify the link status.
        if (linkStatus[0] == 0) {
            // If it failed, delete the program object.
            glDeleteProgram(programObjectId);
            Log.w(TAG, "Linking of program failed.");
            return 0;
        }

        // Return the program object ID.
        return programObjectId;
    }

    /**
     * Validates an OpenGL program. Should only be called when developing the
     * application.
     * <p>
     * cf. https://media.pragprog.com/titles/kbogla/code/AirHockey1/src/com/airhockey/android/util/ShaderHelper.java
     *
     * @param programObjectId OpenGL program object ID to validate
     * @return true if ok
     */
    private static boolean validateProgram(int programObjectId) {
        glValidateProgram(programObjectId);
        final int[] validateStatus = new int[1];
        glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0);
        Log.v(TAG, String.format(
                "Results of validating program: %d\nLog:\n%s",
                validateStatus[0], glGetProgramInfoLog(programObjectId)
        ));

        return validateStatus[0] != 0;
    }
}