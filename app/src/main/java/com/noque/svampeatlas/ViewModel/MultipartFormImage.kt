package com.noque.svampeatlas.ViewModel

import android.graphics.Bitmap
import android.util.Log
import com.noque.svampeatlas.Extensions.toJPEG
import com.noque.svampeatlas.Extensions.toSimpleString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

class MultipartFormImage(private val image: Bitmap,
                         val key: String) {

        val mimeType = "image/jpeg"
        val fileName = "photo-${Calendar.getInstance().time.toSimpleString()}.jpg"
        val byteArray: ByteArray

    init {
        byteArray = image.toJPEG(1.0)
    }
}