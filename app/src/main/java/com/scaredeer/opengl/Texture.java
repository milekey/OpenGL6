package com.scaredeer.opengl;

import static android.opengl.GLES20.GL_NEAREST;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glGenTextures;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLUtils.texImage2D;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class Texture {
    private static final String TAG = "Tile";

    public final int name;

    public Texture(Bitmap bitmap) {
        name = generateTexture(bitmap);
    }

    /**
     * @param bitmap Loads a texture from the Bitmap
     * @return The name (as int) for the corresponding texture object in OpenGL system.
     * Returns 0 if the load failed.
     */
    private int generateTexture(Bitmap bitmap) {
        int[] storedTextureNames = new int[1];
        glGenTextures(1, storedTextureNames, 0);
        if (storedTextureNames[0] == 0) {
            Log.w(TAG, "Could not generate a new OpenGL texture object.");
            return 0;
        }
        int textureName = storedTextureNames[0];


        // Bind to the texture in OpenGL
        glBindTexture(GL_TEXTURE_2D, textureName);

        // Set filtering: a default must be set, or the texture will be black.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        // Load the bitmap into the bound texture.
        texImage2D(GL_TEXTURE_2D, 0, bitmap, 0);

        // Note: Following code may cause an error to be reported in the ADB log as follows:
        // E/IMGSRV(20095): :0: HardwareMipGen: Failed to generate texture mipmap levels (error=3)
        // No OpenGL error will be encountered (glGetError() will return0).
        // If this happens, just squash the source image to be square.
        // It will look the same because of texture coordinates, and mipmap generation will work.
        //glGenerateMipmap(GL_TEXTURE_2D);

        // Recycle the bitmap, since its data has been loaded into OpenGL.
        bitmap.recycle();

        // Unbind from the texture.
        glBindTexture(GL_TEXTURE_2D, 0);


        return textureName;
    }

    public void deleteTexture() {
        int[] storedTextureNames = {name};
        glDeleteTextures(1, storedTextureNames, 0);
    }

    /**
     * リソースから Bitmap を読み込みたい場合に。
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
}
