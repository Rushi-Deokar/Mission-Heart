package com.example.missionheart

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

object ImageUtils {
    /**
     * Resizes and compresses a bitmap to reduce data usage and improve AI processing speed.
     */
    fun compressBitmap(original: Bitmap): Bitmap {
        val maxSize = 1024
        val width = original.width
        val height = original.height

        if (width <= maxSize && height <= maxSize) return original

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (ratio > 1) {
            newWidth = maxSize
            newHeight = (maxSize / ratio).toInt()
        } else {
            newHeight = maxSize
            newWidth = (maxSize * ratio).toInt()
        }

        val resized = Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, out)
        val byteArray = out.toByteArray()
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }
}
