package com.noque.svampeatlas.extensions

import android.graphics.*
import android.media.Image
import android.util.Log
import java.io.ByteArrayOutputStream
import android.graphics.LightingColorFilter
import android.graphics.Bitmap
import android.util.Base64
import com.noque.svampeatlas.utilities.ExifUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


suspend fun Bitmap.toBase64(): String {
   return Base64.encodeToString(this.toJPEG(0.3), Base64.DEFAULT)
}

suspend fun Bitmap.toJPEG(megabyteSize: Double, megabyteDelta: Double = 0.2): ByteArray = withContext(Dispatchers.Default) {
    val allowedSizeInBytes = (megabyteSize * 1024 * 1024).toInt()
    val deltaInBytes = (megabyteDelta * 1024 * 1024)
    val outputStream = ByteArrayOutputStream()
    this@toJPEG.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

    if (outputStream.size() < deltaInBytes + allowedSizeInBytes) {
        return@withContext outputStream.toByteArray()
    } else {
        outputStream.reset()

        var left = 0
        var right = 100
        var mid = (left + right) / 2
        var index = 0

        this@toJPEG.compress(Bitmap.CompressFormat.JPEG, mid, outputStream)

        while (index <= 7) {
            index += 1

            if (outputStream.size() < (allowedSizeInBytes - deltaInBytes)) {
                left = mid
            } else if (outputStream.size() > (allowedSizeInBytes + deltaInBytes)) {
                right = mid
            } else {
                Log.d("Extension", "Compression ran $index times, size is: ${outputStream.size()}")
                return@withContext outputStream.toByteArray()
            }

            mid = (left + right) / 2
            outputStream.reset()
            this@toJPEG.compress(Bitmap.CompressFormat.JPEG, mid, outputStream)
        }

        Log.d("Extension", "Compression ran too many times using compression level: $mid")
        this@toJPEG.compress(Bitmap.CompressFormat.JPEG, mid, outputStream)
        return@withContext outputStream.toByteArray()
    }
}


fun Bitmap.changeColor(color: Int): Bitmap {
    val resultBitmap = Bitmap.createBitmap(
        this, 0, 0,
        this.width - 1, this.height - 1
    )
    val p = Paint()
    val filter = LightingColorFilter(color, 1)
    p.colorFilter = filter
    val canvas = Canvas(resultBitmap)
    canvas.drawBitmap(resultBitmap, 0F, 0F, p)
    return resultBitmap
}


suspend fun Bitmap.rotate(imageFile: File): Bitmap = withContext(Dispatchers.Default) {
    return@withContext ExifUtil.rotateBitmap(imageFile.absolutePath, this@rotate)
}