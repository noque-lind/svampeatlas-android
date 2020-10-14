package com.noque.svampeatlas.utilities

import android.app.Application
import android.util.Log
import com.noque.svampeatlas.services.FileManager

class MyApplication: Application() {
    override fun onCreate() {
        SharedPreferences.init(applicationContext)
        super.onCreate()
    }

    override fun onTerminate() {
        Log.d("APPLICATION", "On terminate")
        FileManager.clearTemporaryFiles()
        super.onTerminate()
    }



}