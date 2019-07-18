package com.noque.svampeatlas.View

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.content_view_layout.view.*

class ContentView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.content_view_layout, this)
    }

    fun configure(title: String?, view: View) {

        if (title == null) {
            contentView_space.visibility = GONE
            contentView_dividerTextView.visibility = View.GONE
        } else {
            contentView_dividerTextView.text = title
        }
        contentView_linearLayout.addView(view)
    }
}