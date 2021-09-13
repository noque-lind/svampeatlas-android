package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.extensions.downloadImage
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.highlighted
import com.noque.svampeatlas.services.DataService
import kotlinx.android.synthetic.main.view_profile_image.view.*



class ProfileImageView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private val imageView: ImageView
    private val textView: TextView

    init {
        val inflater = LayoutInflater.from(getContext())
        val view = inflater.inflate(R.layout.view_profile_image, this)
        imageView = view.profileImageView_imageView
        textView = view.profileImageView_textView
        setupView()
    }

    fun setupView() {
        imageView.clipToOutline = true
        imageView.outlineProvider = object: ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                view?.let {
                        val radius = (view.width / 2).toFloat()
                        outline?.setRoundRect(0,0,view.width, view.height, radius)
                }
            }
        }
    }

    fun configure(initials: String?, imageURL: String?, imageSize: DataService.ImageSize) {
        imageView.setImageDrawable(null)

        if (imageURL != null) {
            imageView.downloadImage(imageSize, imageURL, false)
        }

        textView.text = initials?.toUpperCase()?.highlighted()
    }

    fun configure(initials: String?, @DrawableRes drawableRes: Int) {
        textView.text = initials?.toUpperCase()?.highlighted()
        imageView.setImageResource(drawableRes)
    }
}