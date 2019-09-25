package com.noque.svampeatlas.extensions

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.core.text.italic

import androidx.core.text.bold


fun String.italized(context: Context? = null): SpannableStringBuilder {

        val currentString = this


    val string = SpannableStringBuilder().italic {
        append(currentString)
    }

//    string.setSpan(StyleSpan(ResourcesCompat.getFont(context, R .avenir_next_italic)!!.style), 0, this.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
//    Log.d("Extension", string.toString())
    return string



//    val typeFace = ResourcesCompat.getFont(context, com.noque.svampeatlas.R.font.avenir_next_italic)
//
//    val spannableStringBuilder = SpannableStringBuilder(this)
//    spannableStringBuilder.setSpan(CustomTypefaceSpan("", typeFace!!), 0, this.length, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
////    spannableStringBuilder.setSpan(CustomTypefaceSpan(font), 4, 11, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
//
//
//    Log.d("Extension", spannableStringBuilder.toString())
//    return spannableStringBuilder


//    val family = FontFamily.Builder(Font.Builder("regular.ttf").build())
//        .addFont(Font.Builder("bold.ttf").build()).build()
//    val typeface = Typeface.Builder2(family).build()
//
//    val ssb = SpannableStringBuilder("Hello, World.")
//    ssb.setSpan(StyleSpan(Typeface.Bold), 6, 12, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
//
//    textView.setTypeface(typeface)
//    textView.setText(ssb)
//
//    val family = FontFamily.Builder(Font.Builder() Typeface.createFromAsset(context.assets, "fonts/avenir_next_regular.ttf")).build())


}

fun String.highlighted(): SpannableStringBuilder {
    val currentString = this

    val string = SpannableStringBuilder().bold {
        append(currentString)
    }
    return string
}

fun String.upperCased(): SpannableStringBuilder {
    return SpannableStringBuilder().append(this.capitalize())
}