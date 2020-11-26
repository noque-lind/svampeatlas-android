package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_information.view.*

class InformationView(context: Context?, val attrs: AttributeSet?) : LinearLayout(context, attrs) {

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_information, this)
    }

    fun configure(information: List<Pair<String, String>>) {
        fun addInformation(info: Pair<String, String>) {

            val linearLayout = LinearLayout(context).apply {
                this.layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )

                this.orientation = HORIZONTAL

                val textViewLeft = TextView(context, attrs).apply {
                    this.layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )

                    this.text = info.first
                    this.maxLines = 1
                }

                val textViewRight = TextView(context, attrs).apply {
                    this.layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )

                    this.text = info.second
                    this.maxLines = 1
                    this.gravity = Gravity.RIGHT
                }

                this.addView(textViewLeft)
                this.addView(textViewRight)
            }

            informationView_linearLayout.addView(linearLayout)
        }

        informationView_linearLayout.removeAllViews()

        information.forEach {
            addInformation(it)
        }
    }
}