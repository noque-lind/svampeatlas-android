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

class NewObservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val imageViewLayout = itemView.newObservationItem_imageLayout
    private val imageView = itemView.newObservationItem_imageView
    private val imageLabel = itemView.newObservationItem_imageLabel
    private val smallLabel = itemView.newObservationItem_smallLabel
    private val primaryLabel = itemView.newObservationItem_primaryLabel
    private val statusLabel = itemView.newObservationItem_statusLabel
    private val statusIcon = itemView.newObservationItem_statusIcon
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

        smallLabel.text = String.format(itemView.resources.getString(R.string.notesItem_upperLabel), newObservation.creationDate.toTimeString(), newObservation.locality?.name ?: itemView.resources.getString(R.string.common_unknown_locality))
        primaryLabel.text = newObservation.species?.localizedName ?: newObservation.species?.fullName ?: "-"

        if (newObservation.isComplete()) {
            val color = ContextCompat.getColor(itemView.context, R.color.colorGreen)
            newObservationUploadButton.isEnabled = true
            newObservationUploadButton.alpha = 1f
            statusIcon.setImageResource(R.drawable.glyph_checkmark)
            statusIcon.setColorFilter(color)
            statusLabel.setText(R.string.common_readyforupload)
            statusLabel.setTextColor(color)
        } else {
            val color = ContextCompat.getColor(itemView.context, R.color.colorRed)
            newObservationUploadButton.isEnabled = false
            newObservationUploadButton.alpha = 0.3f
            statusIcon.setImageResource(R.drawable.glyph_denied)
            statusIcon.setColorFilter(color)
            statusLabel.setText(R.string.common_notCompleted)
            statusLabel.setTextColor(color)
        }
    }
}