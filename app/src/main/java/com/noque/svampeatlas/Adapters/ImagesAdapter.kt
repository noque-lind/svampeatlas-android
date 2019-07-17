package com.noque.svampeatlas.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Extensions.downloadImage
import com.noque.svampeatlas.Model.Image
import com.noque.svampeatlas.R
import com.noque.svampeatlas.Services.DataService
import kotlinx.android.synthetic.main.item_image.view.*

class ImagesAdapter(private var images: List<Image>): RecyclerView.Adapter<ImagesAdapter.ViewHolder>() {

    fun configure(images: List<Image>) {
        this.images = images
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_image, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return images.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.configure(images[position])
    }


    class ViewHolder(var view: View): RecyclerView.ViewHolder(view) {
        fun configure(image: Image) {
            view.item_image_imageView.downloadImage(DataService.IMAGESIZE.FULL, image.url)
        }
    }
}