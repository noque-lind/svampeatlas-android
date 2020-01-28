package com.noque.svampeatlas.utilities

import android.graphics.Bitmap
import com.noque.svampeatlas.extensions.toJPEG
import com.noque.svampeatlas.extensions.toSimpleString
import java.util.*

class MultipartFormImage(val byteArray: ByteArray,
                         val key: String) {

        val mimeType = "image/jpeg"
        val fileName = "photo-${Calendar.getInstance().time.toSimpleString()}.jpg"
}