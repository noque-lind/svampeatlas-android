package com.noque.svampeatlas.view_holders

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.extensions.downloadImage
import com.noque.svampeatlas.models.Image
import com.noque.svampeatlas.services.DataService
import kotlinx.android.synthetic.main.item_image.view.*

class ImageViewHolder(val scaleType: ImageView.ScaleType, var view: View): RecyclerView.ViewHolder(view) {

    private val imageView = itemView.imageItem_photoView

    init {
        imageView.scaleType = scaleType

    }

    fun setOnClickListener(listener: View.OnClickListener) {
        imageView.setOnPhotoTapListener { _, _, _ ->
            listener.onClick(itemView)
        }
    }

    fun configure(image: Image) {
        imageView.downloadImage(DataService.ImageSize.FULL, image.url)
    }
}