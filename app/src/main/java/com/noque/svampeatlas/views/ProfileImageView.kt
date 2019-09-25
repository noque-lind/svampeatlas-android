package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.extensions.downloadImage
import com.noque.svampeatlas.R
import com.noque.svampeatlas.services.DataService
import kotlinx.android.synthetic.main.view_profile_image.view.*

class ProfileImageView(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private val imageView: ImageView
    private val textView: TextView

    init {
        val inflater = LayoutInflater.from(getContext())
        val view = inflater.inflate(R.layout.view_profile_image, this)
        imageView = view.profileImageView_imageView
        textView = view.profileImageView_textView
    }

    fun configure(initials: String?, imageURL: String?, imageSize: DataService.ImageSize) {
        if (imageURL != null) {
            imageView.downloadImage(imageSize, imageURL, false, null, true)
        }

        textView.text = initials?.capitalize()
    }

    fun configure(initials: String?, @DrawableRes drawableRes: Int) {
        textView.text = initials
        imageView.setImageResource(drawableRes)
    }
}