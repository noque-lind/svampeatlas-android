package com.noque.svampeatlas.adapters.add_observation.details_picker

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.adapters.PickerAdapter
import com.noque.svampeatlas.models.VegetationType
import com.noque.svampeatlas.view_holders.HeaderViewHolder
import com.noque.svampeatlas.view_holders.ItemViewHolder

class VegetationTypesAdapter: PickerAdapter<VegetationType>() {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> {
                sections.getItem(position)
                    .item.let { holder.configure("- ${it.localizedName}") }
            }
        }
    }
}