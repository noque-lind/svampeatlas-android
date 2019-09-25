package com.noque.svampeatlas.views

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_titles.view.*

class TitlesView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_titles, this)
    }

    fun configure(title: SpannableStringBuilder, subtitle: SpannableStringBuilder?) {
        titlesView_titleTextView.text = title

        if (subtitle != null) {
            titlesView_subtitleTextView.text = subtitle
        } else {
            titlesView_subtitleTextView.visibility = GONE
            titlesView_textViewSpacing.visibility = GONE
        }
    }
}