package com.noque.svampeatlas.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Location
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.views.BlankActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.*

suspend fun File.getBitmap(): Result<Bitmap, AppError> = withContext(Dispatchers.IO) {
    try {
        if (!this@getBitmap.exists()) Result.Error<Bitmap, AppError>(AppError("Sorry", "An error occurred while trying to upload image. It does not exist any longer.", null))
        val bitmap = BitmapFactory.decodeFile(this@getBitmap.absolutePath)
        if (bitmap != null) {
            Result.Success<Bitmap, AppError>(bitmap)
        } else {
            Result.Error<Bitmap, AppError>(AppError("Sorry", "An error occurred while trying to upload image. It does not exist any longer.", null))
        }
    } catch (exception: IllegalArgumentException) {
        Result.Error<Bitmap, AppError>(AppError("Sorry", "An error occurred while trying to upload image. It does not exist any longer.", null))
    }
}

fun File.getExifLocation(): Location? {
ExifInterface(this.inputStream()).apply {
    val lat = latLong?.first()
    val lng = latLong?.last()
    val date = getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let {
        SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault()).parse(it)
    }

    if (lat != null && lng != null) {
        return Location(date ?: Date(), LatLng(lat, lng), 65f)
    } else {
        return null
    }
}
}

//suspend fun File.copyTo(file: File) {
//    inputStream().use { input ->
//        file.outputStream().use { output ->
//            input.copyTo(output)
//        }
//    }
//}


//fun File.copyTo(target: File, overwrite: Boolean = false, cleanCopy: Boolean = true, bufferSize: Int = DEFAULT_BUFFER_SIZE): File {
//    if (!this.exists()) {
//        throw NoSuchFileException(file = this, reason = "The source file doesn't exists.")
//    }
//
//    if (target.exists()) {
//        if (!overwrite)
//            throw FileAlreadyExistsException(file = this, other = target, reason = "The destination file already exists.")
//
//        if (cleanCopy)
//            target.delete()
//    }
//
//    if (this.isDirectory) {
//        if (!target.mkdirs())
//            throw FileSystemException(file = this, other = target, reason = "Failed to create target directory.")
//    } else {
//        target.parentFile?.mkdirs()
//
//        this.inputStream().use { input ->
//            target.outputStream().use { output ->
//                input.copyTo(output, bufferSize)
//            }
//        }
//    }
//
//    return target
//}

suspend fun File.copyTo(file: File): Result<File, AppError> = withContext(Dispatchers.IO) {

    try {
        return@withContext Result.Success<File, AppError>(value = this@copyTo.copyTo(file, false))
    } catch (exception: NoSuchFileException) {
        Result.Error<File, AppError>(AppError("", "", null))
    } catch (exception: IOException) {
        Result.Error<File, AppError>(AppError("", "", null))
    }
    }


