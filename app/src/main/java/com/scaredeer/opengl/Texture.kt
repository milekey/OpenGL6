package com.scaredeer.opengl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.util.Log

private val TAG = Texture::class.simpleName

class Texture(bitmap: Bitmap) {

    companion object {
        /**
         * リソースから Bitmap を読み込みたい場合に。
         *
         * @param [context]    Context オブジェクト
         * @param [resourceId] R.drawable.XXX
         * @return Bitmap オブジェクト
         */
        fun loadBitmap(context: Context, resourceId: Int): Bitmap? {
            val options = BitmapFactory.Options()
            options.inScaled = false

            // Read in the resource
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
            if (bitmap == null) {
                Log.w(TAG, "Resource ID $resourceId could not be decoded.")
                return null
            }
            return bitmap
        }
    }

    val name: Int

    init {
        name = generateTexture(bitmap)
    }

    /**
     * @param [bitmap] Loads a texture from the Bitmap
     * @return The name (as int) for the corresponding texture object in OpenGL system.
     * Returns 0 if loading failed.
     */
    private fun generateTexture(bitmap: Bitmap): Int {
        val storedTextureNames = IntArray(1)
        glGenTextures(1, storedTextureNames, 0)
        if (storedTextureNames[0] == 0) {
            Log.w(TAG, "Could not generate a new OpenGL texture object.")
            return 0
        }
        val textureName = storedTextureNames[0]

        // Bind to the texture in OpenGL
        glBindTexture(GL_TEXTURE_2D, textureName)

        // Set filtering: a default must be set, or the texture will be black.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)

        // Note: Following code may cause an error to be reported in the ADB log as follows:
        // E/IMGSRV(20095): :0: HardwareMipGen: Failed to generate texture mipmap levels (error=3)
        // No OpenGL error will be encountered (glGetError() will return0).
        // If this happens, just squash the source image to be square.
        // It will look the same because of texture coordinates, and mipmap generation will work.
        //glGenerateMipmap(GL_TEXTURE_2D)

        // Unbind from the texture.
        glBindTexture(GL_TEXTURE_2D, 0)
        return textureName
    }

    fun deleteTexture() {
        val storedTextureNames = intArrayOf(name)
        glDeleteTextures(1, storedTextureNames, 0)
    }
}