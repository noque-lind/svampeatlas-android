package com.noque.svampeatlas.View

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.titles_view_layout.view.*

class TitlesView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.titles_view_layout, this)
    }

    fun configure(title: SpannableStringBuilder, subtitle: SpannableStringBuilder?) {
        titlesView_titleTextView.text = title


        Log.d("TEST", "TEST")
        Log.d("TEST", "lol ${subtitle}")
        if (subtitle != null) {
            titlesView_subtitleTextView.text = subtitle
        } else {
            titlesView_subtitleTextView.visibility = GONE
            titlesView_textViewSpacing.visibility = GONE
        }
    }
}