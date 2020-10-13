package com.noque.svampeatlas.extensions

import com.noque.svampeatlas.utilities.SharedPreferences
import java.util.*

fun Locale.isDanish(): Boolean {
    return toLanguageTag() == "da-DK" || SharedPreferences.getAlwaysUseDKNames()
}