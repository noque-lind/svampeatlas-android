package com.noque.svampeatlas.Extensions

import android.text.SpannableStringBuilder
import android.util.Log
import androidx.core.text.italic


fun String.italized(): SpannableStringBuilder {
    val currentString = this
    val string = SpannableStringBuilder().italic {
        append(currentString)
    }
    return string
}

fun String.upperCased(): SpannableStringBuilder {
    return SpannableStringBuilder().append(this.capitalize())
}