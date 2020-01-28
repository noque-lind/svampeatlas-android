package com.noque.svampeatlas.utilities

import android.util.Size
import android.view.*
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import java.lang.ref.WeakReference

/**
 * Builder for [Preview] that takes in a [WeakReference] of the view finder and
 * [PreviewConfig], then instantiates a [Preview] which automatically
 * resizes and rotates reacting to config changes.
 */
class AutoFitPreviewBuilder private constructor(config: PreviewConfig,
                                                viewFinderRef: WeakReference<TextureView>
) {
    /** Public instance of preview use-case which can be used by consumers of this adapter */
    val useCase: Preview

    /** Internal variable used to keep track of the use-case's output rotation */
    private var bufferRotation: Int = 0

    init {
        val viewFinder = viewFinderRef.get() ?: throw IllegalArgumentException(
            "Invalid reference to view finder used"
        )

        useCase = Preview(config)

        // Every time the view finder is updated, recompute layout
        useCase.onPreviewOutputUpdateListener = Preview.OnPreviewOutputUpdateListener {
            val viewFinder = viewFinderRef.get() ?: return@OnPreviewOutputUpdateListener
            viewFinder.id = View.generateViewId()
            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            bufferRotation = it.rotationDegrees
//            val rotation = getDisplaySurfaceRotation(viewFinder.display)
//            updateTransform(viewFinder, rotation, it.textureSize, viewFinderDimens)
        }

        viewFinder.addOnLayoutChangeListener { view, left, top, right, bottom, _, _, _, _ ->
        }
        }


    companion object {
        fun build(config: PreviewConfig, viewFinder: TextureView) =
            AutoFitPreviewBuilder(config, WeakReference(viewFinder)).useCase
    }
}