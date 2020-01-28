package com.noque.svampeatlas.extensions

import android.util.Log
import com.noque.svampeatlas.models.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

suspend fun InputStream.copyTo(file: File) = withContext(Dispatchers.IO) {
    this@copyTo.use { input ->
        file.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}