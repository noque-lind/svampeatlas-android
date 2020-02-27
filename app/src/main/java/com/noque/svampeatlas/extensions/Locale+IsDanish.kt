package com.noque.svampeatlas.extensions

import android.util.Log
import java.util.*

fun Locale.isDanish(): Boolean {
    return toLanguageTag() == "da-DK"
}