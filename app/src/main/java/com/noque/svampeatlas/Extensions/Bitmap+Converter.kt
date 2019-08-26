package com.noque.svampeatlas.Extensions

import android.graphics.*
import android.media.Image
import android.util.Log
import java.io.ByteArrayOutputStream
import android.graphics.LightingColorFilter
import android.graphics.ColorFilter
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.graphics.Bitmap



fun Image.toBitmap(): Bitmap {
    val yBuffer = planes[0].buffer // Y
    val uBuffer = planes[1].buffer // U
    val vBuffer = planes[2].buffer // V

    val ySize = yBuffer.remaining()
    val uSize = uBuffer.remaining()
    val vSize = vBuffer.remaining()

    val nv21 = ByteArray(ySize + uSize + vSize)

    //U and V are swapped
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)
    uBuffer.get(nv21, ySize + vSize, uSize)

    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val out = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 50, out)
    val imageBytes = out.toByteArray()
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}

fun Bitmap.toJPEG(megabyteSize: Double, megabyteDelta: Double = 0.2): ByteArray {
    val allowedSizeInBytes = (megabyteSize * 1024 * 1024).toInt()
    val deltaInBytes = (megabyteDelta * 1024 * 1024)
    val outputStream = ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

    if (outputStream.size() < deltaInBytes + allowedSizeInBytes) {
        return outputStream.toByteArray()
    } else {
        outputStream.reset()

        var left = 0
        var right = 100
        var mid = (left + right) / 2
        var index = 0

        this.compress(Bitmap.CompressFormat.JPEG, mid, outputStream)

        while (index <= 7) {
            index += 1

            if (outputStream.size() < (allowedSizeInBytes - deltaInBytes)) {
                left = mid
            } else if (outputStream.size() > (allowedSizeInBytes + deltaInBytes)) {
                right = mid
            } else {
                Log.d("Extension", "Compression ran $index times")
                return outputStream.toByteArray()
            }

            mid = (left + right) / 2
            outputStream.reset()
            this.compress(Bitmap.CompressFormat.JPEG, mid, outputStream)
        }

        Log.d("Extension", "Compression ran too many times")
        this.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        return outputStream.toByteArray()
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