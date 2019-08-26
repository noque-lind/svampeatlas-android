package com.noque.svampeatlas.ViewHolders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Model.Observation
import kotlinx.android.synthetic.main.item_observation.view.*

class ObservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val observationView = itemView.observationItem_observationView

    fun configure(observation: Observation) {
        observationView.configure(observation)
    }

}