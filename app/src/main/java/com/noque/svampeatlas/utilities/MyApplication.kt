package com.noque.svampeatlas.utilities

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.bumptech.glide.load.engine.Resource
import com.downloader.PRDownloader
import com.noque.svampeatlas.services.FileManager
import dagger.hilt.android.HiltAndroidApp

class MyApplication: Application() {

    companion object {
        lateinit var applicationContext: Context
        private set
            lateinit var resources: Resources
            private set
    }


    override fun onCreate() {
        MyApplication.applicationContext = applicationContext
        MyApplication.resources = resources
        SharedPreferences.init(applicationContext)
        PRDownloader.initialize(applicationContext);
        super.onCreate()
    }

    override fun onTerminate() {
        FileManager.clearTemporaryFiles()
        super.onTerminate()
    }



}