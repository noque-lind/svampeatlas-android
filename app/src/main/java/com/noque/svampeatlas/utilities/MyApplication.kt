package com.noque.svampeatlas.utilities

import android.app.Application
import android.content.Context
import android.util.Log
import com.noque.svampeatlas.services.FileManager

class MyApplication: Application() {

    companion object {
        lateinit var applicationContext: Context
            private set
    }


    override fun onCreate() {
        MyApplication.applicationContext = applicationContext
        SharedPreferences.init(applicationContext)
        super.onCreate()
    }

    override fun onTerminate() {
        FileManager.clearTemporaryFiles()
        super.onTerminate()
    }



}