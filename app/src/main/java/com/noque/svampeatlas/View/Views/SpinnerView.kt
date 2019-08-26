package com.noque.svampeatlas.View.Views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_background.view.*
import kotlinx.android.synthetic.main.view_spinner.view.*

class SpinnerView(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private val progressBar by lazy { spinnerView_progressBar }

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_spinner, this)
    }

    fun startLoading() {
        isClickable = true
        isFocusable = true
        val color = ColorUtils.setAlphaComponent(Color.BLACK, 150)
        setBackgroundColor(color)
        progressBar.visibility = View.VISIBLE
    }

    fun stopLoading() {
        isClickable = false
        isFocusable = false

        setBackgroundColor(Color.TRANSPARENT)
        progressBar.visibility = View.GONE
    }
}