package com.noque.svampeatlas.models

import android.content.Context
import android.content.res.Resources
import com.noque.svampeatlas.R

enum class RecoveryAction {
    OPENSETTINGS,
    TRYAGAIN,
    LOGIN,
    ACTIVATE;

    fun description(resources: Resources): String {
        return when(this) {
            ACTIVATE -> resources.getString(R.string.recovery_action_activate)
            OPENSETTINGS -> resources.getString(R.string.recovery_action_openSettings)
            TRYAGAIN -> resources.getString(R.string.recovery_action_tryAgain)
            LOGIN -> resources.getString(R.string.recovery_action_login)
        }
    }
}

open class AppError(val title: String, val message: String, val recoveryAction: RecoveryAction?)