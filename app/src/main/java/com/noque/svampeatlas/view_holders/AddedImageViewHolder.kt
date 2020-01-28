package com.noque.svampeatlas.view_holders

import android.graphics.Bitmap
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.downloadImage
import kotlinx.android.synthetic.main.item_added_image.view.*
import java.io.File

class AddedImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    var imageView: ImageView

    init {
        imageView = itemView.addedImageItem_imageView
        imageView.clipToOutline = true
        imageView.outlineProvider = object: ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                view?.let {
                    val radius = itemView.resources.getDimension(R.dimen.app_rounded_corners)
                    outline?.setRoundRect(0,0,view.width, view.height, radius)
                }
            }
        }
    }

    fun configure(imageFile: File) {
        Glide.with(imageView)
            .load(imageFile)
            .into(imageView)
    }
}