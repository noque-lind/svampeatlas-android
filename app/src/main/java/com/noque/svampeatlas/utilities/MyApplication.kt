package com.noque.svampeatlas.utilities

import android.app.Application

class MyApplication: Application() {
    override fun onCreate() {
        SharedPreferences.init(applicationContext)
        super.onCreate()
    }
}