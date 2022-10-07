package com.noque.svampeatlas.services

import android.content.Context
import android.media.MediaScannerConnection
import android.webkit.MimeTypeMap
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.copyTo
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Result
import com.noque.svampeatlas.utilities.MyApplication
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileManager {

    private val temporaryFiles = mutableListOf<File>()

    fun createTempFile(context: Context): File {
        val file = File.createTempFile(
            SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS",
                Locale.getDefault()
            ).format(System.currentTimeMillis()), ".jpg", getCacheDir(context)
        )
        temporaryFiles.add(file)
        return file
    }

    fun createDocumentFile(name: String, context: Context): File {
        return File.createTempFile( SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(System.currentTimeMillis()) + " - $name", ".json", getCacheDir(context))
    }


    fun createFile(context: Context): File = File(
        getOutputDirectory(context), SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SSS",
            Locale.getDefault()
        ).format(System.currentTimeMillis()) + ".jpg"
    )

    suspend fun saveAsNotesImage(file: File): Result<File, AppError> {
        return file.copyTo(createNotesDirectoryImageFile(MyApplication.applicationContext)).apply {
            onSuccess { scanImageForGallery(it, MyApplication.applicationContext) }
        }
    }

    fun deleteImageGalleryFile(file: File) {
        file.delete()
        scanImageForGallery(file, MyApplication.applicationContext)
    }

    fun createNotesDirectoryImageFile(context: Context): File = File(getOutputDirectoryForNoteImages(context),SimpleDateFormat(
        "yyyy-MM-dd-HH-mm-ss-SSS",
        Locale.getDefault()
    ).format(System.currentTimeMillis()) + ".jpg")

    fun clearTemporaryFiles() {
        temporaryFiles.forEach { it.delete() }
    }


    private fun getOutputDirectory(context: Context): File {
        val appContext = context.applicationContext
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
        }

        return if (mediaDir != null && mediaDir.exists()) mediaDir else appContext.filesDir
    }

    private fun getOutputDirectoryForNoteImages(context: Context): File {
        val appContext = context.applicationContext
        val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
            File(it, appContext.resources.getString(R.string.app_name) + " - Notebook").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else appContext.filesDir
    }

    private fun getCacheDir(context: Context): File {
        val appContext = context.applicationContext
        val cacheDir = context.externalCacheDirs.firstOrNull()
        return if (cacheDir != null && cacheDir.exists()) cacheDir else appContext.cacheDir
    }



    suspend fun saveTempImage(tempImageFile: File, toNewFile: File, context: Context): Result<File, AppError> {
        val result = tempImageFile.copyTo(toNewFile)
        result.onSuccess {
            scanImageForGallery(it, context)
        }
        return result
    }

    private fun scanImageForGallery(file: File, context: Context) {
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(mimeType),
            null
        )
    }
}