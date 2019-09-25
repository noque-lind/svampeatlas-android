package com.noque.svampeatlas.utilities

import android.graphics.Bitmap
import com.noque.svampeatlas.extensions.toJPEG
import com.noque.svampeatlas.extensions.toSimpleString
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