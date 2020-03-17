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

    private val captureButton = cameraControlsView_captureButton
    private val photoLibraryButton = cameraControlsView_libraryButton

    init {
        val inflater = LayoutInflater.from(context)
        inflater.inflate(R.layout.view_camera_controls, this)
    }
}