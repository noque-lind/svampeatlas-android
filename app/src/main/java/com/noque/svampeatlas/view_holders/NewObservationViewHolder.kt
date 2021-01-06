package com.noque.svampeatlas.view_holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.models.NewObservation
import kotlinx.android.synthetic.main.item_new_observation.view.*

class NewObservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val smallLabel = itemView.newObservationItem_smallLabel
    private val primaryLabel = itemView.newObservationItem_primaryLabel

    fun configure(newObservation: NewObservation) {
        smallLabel.text = newObservation.locality?.name
        primaryLabel.text = newObservation.species?.localizedName ?: newObservation.species?.fullName ?: "Ikke valgt art"
    }
}