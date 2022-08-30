package com.noque.svampeatlas.extensions

import android.util.Log
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit



fun Date(ISO8601: String?): Date? {
    ISO8601?.let {
        val cm = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        cm.timeZone = TimeZone.getTimeZone("UTC")
        return try {
            cm.parse(ISO8601)
        } catch (error: ParseException) {
            Log.d("Extension", error.toString())
            null
        }
    }

    return null
}

fun Date(minusMonths: Int): Date {
    val cal = Calendar.getInstance()
    cal.setTime(Date())
    cal.add(Calendar.MONTH, -minusMonths)
    return cal.time
}

fun Date.toDatabaseName(): String {
    val sf = SimpleDateFormat("yyyy-MM-dd")
    return sf.format(this)
}

fun String.toDate(): Date {
    val sf = SimpleDateFormat("yyyy-MM-dd")
    return sf.parse(this)
}


fun Date.toISO8601(): String {
    val cm = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    cm.timeZone = TimeZone.getTimeZone("UTC")
   return cm.format(this)
}

fun Date.difDays(): Long {
    return TimeUnit.MILLISECONDS.toDays(
        Calendar.getInstance().time.time - this.time
    )
}

fun Date.difHours(): Long {
    return TimeUnit.HOURS.toHours(Calendar.getInstance().time.time - this.time)
}

fun Date.toTimeString(): String {
    return SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT).format(this)
}

fun Date.toReadableDate(recentFormatting: Boolean = true, ignoreTime: Boolean = false): String {
    if (!recentFormatting) {
        return if (ignoreTime) {
            SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM).format(this)
        } else {
            SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.SHORT).format(
                this
            )
        }
    } else {
        val diff = Calendar.getInstance().time.time - this.time
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        if (days < 15L) {
            if (days == 0L && !ignoreTime) {
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                return if (hours == 0L) "For nyligt" else "$hours timer siden"
            } else if (days == 0L) {
                return "I dag"
            } else if (days == 1L) {
                return "1 dag siden"
            } else {
                return "$days dage siden"
            }
        } else {
            return if (ignoreTime) {
                SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM).format(this)
            } else {
                SimpleDateFormat.getDateTimeInstance(
                    SimpleDateFormat.MEDIUM,
                    SimpleDateFormat.SHORT
                ).format(this)
            }
        }
    }
}

//func Date.checkIfDateIsRecent(ignoreTime: Bool, date: Date) -> String?  {
//    let components = NSCalendar.current.dateComponents([Calendar.Component.day, Calendar.Component.hour], from: date, to: self)
//    if let days = components.day, days < 30 {
//        if days == 0 {
//            if let hours = components.hour, ignoreTime == false {
//            if hours == 0 {
//                return "Lige nu"
//            } else {
//                return "\(hours) timer siden"
//            }
//        } else {
//            return "I dag"
//        }
//        } else if days == 1 {
//            return "1 dag siden"
//        } else {
//            return "\(days) dage siden"
//        }
//    } else {
//        return nil
//    }
//}