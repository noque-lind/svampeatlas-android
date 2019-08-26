package com.noque.svampeatlas.Extensions

import android.util.Log
import com.android.volley.ParseError
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


fun Date(ISO8601: String): Date {
//    Log.d("Extension", ISO8601)


    val cm = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

        try {
           return cm.parse(ISO8601)!!
        } catch (error: ParseException) {
            return Calendar.getInstance().time
        }
}

fun Date.toSimpleString(): String {
    val sf = SimpleDateFormat("yyyy-MM-dd")
    return sf.format(this)
}

fun Date.toReadableDate(): String {
    val sf = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.SHORT)
    return sf.format(this)
}