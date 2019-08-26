package com.noque.svampeatlas.Adapters.AddObservationAdapters

import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.ViewHolders.AddImageViewHolder
import com.noque.svampeatlas.ViewHolders.AddedImageViewHolder

class AddImagesAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val TAG = "AddImagesAdapter"
    }

    enum class ViewType {
        ADDIMAGEVIEW,
        ADDIMAGEVIEWEXPANDED,
        ADDEDIMAGEVIEW;

        companion object {
            val values = values()
        }
    }

    var addImageButtonClicked: (() -> Unit)? = null

    private var images = listOf<Bitmap>()

    private val onClickListener = object: View.OnClickListener {
        override fun onClick(view: View?) {
            Log.d(TAG, "OnClick")
            addImageButtonClicked?.invoke()
        }

    }

    fun configure(images: List<Bitmap>) {
        Log.d("what", images.toString())
        this.images = images
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if (position < images.count()) {
            return ViewType.ADDEDIMAGEVIEW.ordinal
        } else {
            return if (images.count() == 0) ViewType.ADDIMAGEVIEWEXPANDED.ordinal else ViewType.ADDIMAGEVIEW.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View
        val viewHolder: RecyclerView.ViewHolder
        val layoutInflater = LayoutInflater.from(parent.context)

        when (ViewType.values[viewType]) {
            ViewType.ADDIMAGEVIEW -> {
                view = layoutInflater.inflate(R.layout.item_add_image, parent, false)
                viewHolder = AddImageViewHolder(view)
                viewHolder.itemView.setOnClickListener(onClickListener)
            }

            ViewType.ADDIMAGEVIEWEXPANDED -> {
                view = layoutInflater.inflate(R.layout.item_add_image_expanded, parent, false)
                viewHolder = AddImageViewHolder(view)
                viewHolder.itemView.setOnClickListener(onClickListener)
            }

            ViewType.ADDEDIMAGEVIEW -> {
                view = layoutInflater.inflate(R.layout.item_added_image, parent, false)
                viewHolder = AddedImageViewHolder(view)
            }
        }

        return viewHolder
    }

    override fun getItemCount(): Int {
        return images.count() + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (position < images.count()) {
            (holder as? AddedImageViewHolder)?.configure(images[position])
        } else {
//            val holder = holder as? AddImageViewHolder
//
//            if (images.count() == 0) {
//                holder?.collapse()
//            } else {
//                holder?.expand()
//            }
        }

    }
}