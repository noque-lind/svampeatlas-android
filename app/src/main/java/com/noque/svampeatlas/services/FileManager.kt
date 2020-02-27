package com.noque.svampeatlas.services

import android.content.Context
import com.noque.svampeatlas.R
import com.noque.svampeatlas.views.BlankActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileManager {

    private val temporaryFiles = mutableListOf<File>()

    fun createTempFile(context: Context): File {
        val file = File.createTempFile(SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis()), ".jpg", getCacheDir(context))
        temporaryFiles.add(file)
        return file
    }

    fun createFile(context: Context): File = File(getOutputDirectory(context), SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis()) + ".jpg")

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

    private fun getCacheDir(context: Context): File {
        val appContext = context.applicationContext
        val cacheDir = context.externalCacheDirs.firstOrNull()
        return if (cacheDir != null && cacheDir.exists()) cacheDir else appContext.cacheDir
    }


}