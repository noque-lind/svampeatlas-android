package com.noque.svampeatlas.view_holders

import android.content.Context
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.extensions.toTimeString
import com.noque.svampeatlas.models.NewObservation
import kotlinx.android.synthetic.main.item_new_observation.view.*

class NoteItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val imageViewLayout = itemView.newObservationItem_imageLayout
    private val imageView = itemView.newObservationItem_imageView
    private val imageLabel = itemView.newObservationItem_imageLabel
    private val smallLabel = itemView.newObservationItem_smallLabel
    private val primaryLabel = itemView.newObservationItem_primaryLabel
    private val newObservationUploadButton = itemView.newObservationItem_uploadButton

    fun configure(newObservation: NewObservation, onUploadButtonClick: (() -> Unit)) {
        newObservationUploadButton.setOnClickListener {
            onUploadButtonClick()
        }

        if (!newObservation.images.isEmpty()) {
            imageViewLayout.visibility = View.VISIBLE

            Glide.with(imageView)
                .load(newObservation.images.first())

                .into(imageView)

            if (newObservation.images.count() > 1) {
                imageLabel.text = "+ ${newObservation.images.count() - 1}"
                imageLabel.visibility = View.VISIBLE
            } else {
                imageLabel.visibility = View.GONE
            }
        } else {
            imageViewLayout.visibility = View.GONE
        }

        smallLabel.text = newObservation.creationDate.toTimeString() + ", " +  (newObservation.locality?.name ?: itemView.resources.getString(R.string.common_localityNotSelected))
        primaryLabel.text = newObservation.species?.localizedName ?: newObservation.species?.fullName ?: "-"
    }
}