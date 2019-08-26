package com.noque.svampeatlas.ViewHolders

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_added_image.view.*

class AddedImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var imageView: ImageView

    init {
        imageView = itemView.addedImageItem_imageView
    }

    fun configure(image: Bitmap) {
        Glide.with(imageView)
            .load(image)
            .into(imageView)
    }
}