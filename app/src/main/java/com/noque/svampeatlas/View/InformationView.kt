package com.noque.svampeatlas.View

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.information_view_layout.view.*

class InformationView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.information_view_layout, this)
    }

    private fun configure(information: List<Pair<String, String>>) {
        fun addInformation(info: Pair<String, String>) {
            val linearLayout = LinearLayout(context)
            linearLayout.layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            linearLayout.orientation = LinearLayout.HORIZONTAL

            val textViewLeft = TextView(context)
            textViewLeft.setTextAppearance(context, R.style.AppPrimary)
            textViewLeft.layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            textViewLeft.text = info.first
            textViewLeft.maxLines = 1

            val textViewRight = TextView(context)
            textViewRight.setTextAppearance(context, R.style.AppPrimary)
            textViewRight.layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            textViewRight.text = info.second
            textViewRight.gravity = Gravity.RIGHT
            textViewRight.maxLines = 1

            linearLayout.addView(textViewLeft)
            linearLayout.addView(textViewRight)
            informationView_linearLayout.addView(linearLayout)
        }

        informationView_linearLayout.removeAllViews()

        information.forEach {
            addInformation(it)
        }
    }
}