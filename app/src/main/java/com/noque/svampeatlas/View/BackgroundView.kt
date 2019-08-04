package com.noque.svampeatlas.View

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.Model.AppError
import kotlinx.android.synthetic.main.view_background.view.*

class BackgroundView(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_background, this)
    }

    fun setLoading() {
        backgroundView_progressBar.visibility = View.VISIBLE
    }

    fun setError(error: AppError) {
        backgroundView_errorView_titleTextView.text = error.title
        backgroundView_errorView_messageTextView.text = error.message
        backgroundView_errorView_linearLayout.visibility = View.VISIBLE
    }

    fun reset() {
        backgroundView_errorView_linearLayout.visibility = View.GONE
        backgroundView_progressBar.visibility = View.GONE
    }
}