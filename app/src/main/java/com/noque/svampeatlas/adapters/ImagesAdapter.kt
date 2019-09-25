package com.noque.svampeatlas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.Image
import com.noque.svampeatlas.R
import com.noque.svampeatlas.view_holders.ImageViewHolder

class ImagesAdapter: RecyclerView.Adapter<ImageViewHolder>() {

    private var images = listOf<Image>()
    private var scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP

    var onClickedAtIndex: ((Int) -> Unit)? = null

    private val onClickListener = View.OnClickListener {
        (it.tag as? RecyclerView.ViewHolder)?.adapterPosition?.let {
            onClickedAtIndex?.invoke(it)
        }
    }

    fun configure(images: List<Image>, scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP) {
        this.scaleType = scaleType
        this.images = images
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_image, parent, false)
        val imageViewHolder = ImageViewHolder(scaleType, view)
        imageViewHolder.itemView.tag = imageViewHolder
        imageViewHolder.setOnClickListener(onClickListener)
        return imageViewHolder
    }

    override fun getItemCount(): Int {
        return images.count()
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.configure(images[position])
    }
}