package com.noque.svampeatlas.extensions

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun File.getBitmap(): Bitmap = withContext(Dispatchers.IO) {
    return@withContext BitmapFactory.decodeFile(this@getBitmap.absolutePath)
}