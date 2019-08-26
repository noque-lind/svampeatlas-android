package com.noque.svampeatlas.View.Views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.Extensions.downloadImage
import com.noque.svampeatlas.R
import com.noque.svampeatlas.Services.DataService
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

    fun configure(initials: String?, imageURL: String?) {
        if (imageURL != null) {
            imageView.downloadImage(DataService.IMAGESIZE.FULL, imageURL)
        } else {
            imageView.setImageResource(R.mipmap.android_app)
        }

        textView.text = initials?.capitalize()
    }
}