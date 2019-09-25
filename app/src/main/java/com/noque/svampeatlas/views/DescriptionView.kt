package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_description.view.*

class DescriptionView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_description, this)
    }

    fun configure(title: String?, content: String) {
        if (title == null) {
            descriptionView_dividerTextView.visibility = GONE
        } else {
            descriptionView_dividerTextView.text = title
        }

        descriptionView_contentTextView.text = content
    }

}