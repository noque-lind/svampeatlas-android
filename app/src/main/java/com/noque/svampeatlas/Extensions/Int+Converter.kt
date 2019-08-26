package com.noque.svampeatlas.Extensions

import android.content.Context

fun Int.dpToPx(context: Context?): Int {
    return this * ((context?.resources?.displayMetrics?.density?.toInt()) ?: this)
}

fun Int.pxToDp(context: Context?): Int {
    return this * ((context?.resources?.displayMetrics?.density?.toInt()) ?: this)
}