package com.noque.svampeatlas.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_camera_controls.view.*

class CameraControlsView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    interface Listener {
        fun captureButtonPressed()
        fun resetButtonPressed()
        fun photoLibraryButtonPressed()
        fun actionButtonPressed(state: State)
    }

    enum class State {
        CAPTURE_NEW,
        CAPTURE,
        LOADING,
        CONFIRM,
        HIDDEN
    }

    private var listener: Listener? = null
    private var state: State = State.HIDDEN

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
            if (state == State.CONFIRM) listener?.resetButtonPressed() else listener?.photoLibraryButtonPressed()
        }

        actionButton.setOnClickListener {
            listener?.actionButtonPressed(state)
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun configureState(state: State) {
        this.state = state
        visibility = View.VISIBLE
        when (state) {
            State.CAPTURE_NEW -> {
                photoLibraryButton.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_photo_library, null))
                spinner.visibility = View.INVISIBLE
                captureButton.visibility = View.VISIBLE
                photoLibraryButton.visibility = View.VISIBLE
                actionButton.visibility = View.VISIBLE
                actionButton.setText(R.string.cameraControlTextButton_noPhoto)
            }
            State.LOADING -> {
                captureButton.visibility = View.INVISIBLE
                spinner.visibility = View.VISIBLE
                photoLibraryButton.visibility = View.INVISIBLE
                actionButton.visibility = View.INVISIBLE
            }

            State.CAPTURE -> {
                photoLibraryButton.visibility = View.VISIBLE
                captureButton.visibility = View.VISIBLE
                spinner.visibility = View.INVISIBLE
                actionButton.visibility = View.INVISIBLE
            }

            State.CONFIRM -> {
                captureButton.visibility = View.INVISIBLE
                spinner.visibility = View.INVISIBLE
                photoLibraryButton.visibility = View.VISIBLE
                photoLibraryButton.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.icon_back_button, null))
                actionButton.setText(R.string.cameraControlTextButton_usePhoto)
                actionButton.visibility = View.VISIBLE
            }
            State.HIDDEN -> {
                visibility = View.GONE
            }
        }
    }

    fun rotate(transform: Float, animationDuration: Long) {
        photoLibraryButton.animate().rotation(transform).setDuration(animationDuration).start()
        actionButton.animate().rotation(transform).setDuration(animationDuration).start()
    }
}