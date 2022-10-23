package com.noque.svampeatlas.utilities

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.downloader.PRDownloader
import com.logrocket.core.Configuration
import com.logrocket.core.SDK
import com.noque.svampeatlas.services.FileManager


class MyApplication: Application() {

    companion object {
        lateinit var applicationContext: Context
        private set
            lateinit var resources: Resources
            private set
    }


    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        SDK.init(
            this,
            base
        ) { options: Configuration -> options.setAppID("nluvzb/svampeatlas-android"); options.setEnableViewScanning(false); options.setEnableIPCapture(false) }
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