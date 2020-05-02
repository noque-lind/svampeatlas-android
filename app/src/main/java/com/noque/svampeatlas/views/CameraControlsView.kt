package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_camera_controls.view.*

class CameraControlsView(context: Context?, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    interface Listener {
        fun captureButtonPressed()
        fun resetButtonPressed()
        fun photoLibraryButtonPressed()
    }

    enum class State {
        CAPTURE_NEW,
        CAPTURE,
        LOADING,
        CONFIRM,
        HIDDEN
    }

    private var listener: Listener? = null

    private val captureButton by lazy {cameraControlsView_captureButton }
    private val photoLibraryButton by lazy {cameraControlsView_libraryButton }
    private val actionButton by lazy { cameraControlsView_noPhotoButton }
    private val spinner by lazy { cameraControlsView_captureButtonSpinner }

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.view_camera_controls, this)
        setupView()
    }

    private fun setupView() {
        captureButton.setOnClickListener {
            listener?.captureButtonPressed()
        }

        photoLibraryButton.setOnClickListener {
            listener?.photoLibraryButtonPressed()
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun configureState(state: State) {
        visibility = View.VISIBLE
        when (state) {
            State.CAPTURE_NEW -> {
                spinner.visibility = View.GONE
                captureButton.visibility = View.VISIBLE
                photoLibraryButton.visibility = View.VISIBLE
                actionButton.visibility = View.VISIBLE
                actionButton.setText(R.string.cameraControlTextButton_noPhoto)
            }
            State.LOADING -> {
                captureButton.visibility = View.GONE
                spinner.visibility = View.VISIBLE
                photoLibraryButton.visibility = View.GONE
                actionButton.visibility = View.GONE
            }

            State.CAPTURE -> {
                captureButton.visibility = View.VISIBLE
                spinner.visibility = View.GONE
                photoLibraryButton.visibility = View.GONE
                actionButton.visibility = View.GONE
            }

            State.CONFIRM -> {
                captureButton.visibility = View.GONE
                spinner.visibility = View.GONE
                photoLibraryButton.visibility = View.VISIBLE
                photoLibraryButton.setImageDrawable(resources.getDrawable(R.drawable.icon_back_button, null))
                actionButton.setText(R.string.cameraControlTextButton_usePhoto)
            }
            State.HIDDEN -> {
                visibility = View.GONE
            }
        }
    }
}