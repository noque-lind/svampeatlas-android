package com.noque.svampeatlas.View

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.load.engine.Resource
import com.noque.svampeatlas.R
import com.noque.svampeatlas.Model.AppError
import kotlinx.android.synthetic.main.view_background.view.*

class BackgroundView(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private val titleTextView by lazy {backgroundView_errorView_titleTextView}
    private val messageTextView by lazy {backgroundView_errorView_messageTextView}
    private val handlerButton by lazy {backgroundView_handlerButton}
    private val errorLinearLayout by lazy {backgroundView_errorView_linearLayout}

    private val spinnerView by lazy {backgroundView_spinnerView}


    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_background, this)
    }

    fun setLoading() {
        spinnerView.startLoading()
    }

    fun setError(error: AppError) {
        backgroundView_errorView_titleTextView.text = error.title
        backgroundView_errorView_messageTextView.text = error.message
        backgroundView_errorView_linearLayout.visibility = View.VISIBLE
    }

    fun setErrorWithHandler(error: AppError, handlerTitle: String, handler: (() -> Unit)) {
        errorLinearLayout.visibility = View.VISIBLE

        titleTextView.text = error.title
        messageTextView.text = error.message
        handlerButton.visibility = View.VISIBLE
        handlerButton.text = handlerTitle
        handlerButton.setOnClickListener {
            handler.invoke()
        }
    }

    fun reset() {
        setBackgroundColor(Color.TRANSPARENT)
        backgroundView_errorView_linearLayout.visibility = View.GONE
        spinnerView.stopLoading()
    }
}