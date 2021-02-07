package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import com.noque.svampeatlas.R


class RoundedImageView(context: Context, attrs: AttributeSet?) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs) {


    override fun onAttachedToWindow() {
        clipToOutline = true
        outlineProvider = object: ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                view?.let {
                    val radius = resources.getDimension(R.dimen.app_rounded_corners)
                    outline?.setRoundRect(0, 0, view.width, view.height, radius)
                }
            }
        }
        super.onAttachedToWindow()
    }
}