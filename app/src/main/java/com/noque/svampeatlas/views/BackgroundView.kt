package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.RecoveryAction
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
        errorLinearLayout.visibility = View.GONE

        backgroundView_errorView_titleTextView.text = error.title
        backgroundView_errorView_messageTextView.text = error.message
        backgroundView_errorView_linearLayout.visibility = View.VISIBLE
    }

    fun setErrorWithHandler(error: AppError, recoveryAction: RecoveryAction?, handler: ((RecoveryAction?) -> Unit)) {
        errorLinearLayout.visibility = View.VISIBLE

        titleTextView.text = error.title
        messageTextView.text = error.message
        handlerButton.visibility = View.VISIBLE
        handlerButton.text = recoveryAction?.description(resources) ?: RecoveryAction.TRYAGAIN.description(resources)
        handlerButton.setOnClickListener {
            handler.invoke(recoveryAction)
        }
    }

    fun reset() {
        setBackgroundColor(Color.TRANSPARENT)
        backgroundView_errorView_linearLayout.visibility = View.GONE
        spinnerView.stopLoading()
    }
}