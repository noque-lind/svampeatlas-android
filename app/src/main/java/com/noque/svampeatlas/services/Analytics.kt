package com.noque.svampeatlas.services

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

object Analytics {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    fun setInstance(mObject: FirebaseAnalytics) {
        firebaseAnalytics = mObject
    }

    fun logEvent(name: String, params: Bundle) {
        firebaseAnalytics.logEvent(name, params)
    }
}