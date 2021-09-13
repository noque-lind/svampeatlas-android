package com.noque.svampeatlas.utilities

import com.noque.svampeatlas.extensions.toDatabaseName
import java.util.*

class MultipartFormImage(val byteArray: ByteArray,
                         val key: String) {

        val mimeType = "image/jpeg"
        val fileName = "photo-${Calendar.getInstance().time.toDatabaseName()}.jpg"
}