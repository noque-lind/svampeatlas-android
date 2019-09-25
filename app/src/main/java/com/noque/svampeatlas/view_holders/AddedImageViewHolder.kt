package com.noque.svampeatlas.view_holders

import android.graphics.Bitmap
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.item_added_image.view.*
import java.io.File

class AddedImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var imageView: ImageView

    init {
        imageView = itemView.addedImageItem_imageView
    }

    fun configure(imageFile: File) {
        itemView.alpha = 1F

        Glide.with(imageView)
            .load(imageFile)
            .transform(CenterCrop(), RoundedCorners(itemView.resources.getDimensionPixelSize(R.dimen.secondary_color_rounded)))
            .into(imageView)
    }
}