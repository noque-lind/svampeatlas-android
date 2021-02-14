package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_header.view.*

class HeaderView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val textView: TextView
    private val extraTextView: TextView

    init {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.view_header, this)
        textView = view.headerView_textView
        extraTextView = view.headerView_extraTextView
    }

    fun configure(title: String, extraMessage: String? = null) {
        textView.text = title
        extraTextView.text = extraMessage
    }
}