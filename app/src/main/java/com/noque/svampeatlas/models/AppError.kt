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
            ACTIVATE -> resources.getString(R.string.recoveryAction_activate)
            OPENSETTINGS -> resources.getString(R.string.recoveryAction_openSettings)
            TRYAGAIN -> resources.getString(R.string.recoveryAction_tryAgain)
            LOGIN -> resources.getString(R.string.recoveryAction_login)
        }
    }
}

open class AppError(val title: String, val message: String, val recoveryAction: RecoveryAction?)
open class AppError2(val title: Int, val message: Int, val recoveryAction: RecoveryAction?) {
    fun toAppError(resources: Resources): AppError {
        return AppError(resources.getString(title), resources.getString(message), recoveryAction)
    }
}