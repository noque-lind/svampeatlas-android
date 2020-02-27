package com.noque.svampeatlas.adapters.add_observation.details_picker

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.adapters.PickerAdapter
import com.noque.svampeatlas.models.Substrate
import com.noque.svampeatlas.view_holders.HeaderViewHolder
import com.noque.svampeatlas.view_holders.ItemViewHolder

class SubstratesAdapter(): PickerAdapter<Substrate>() {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> {
                sections.getItem(position)
                    .item.let { holder.configure("- ${it.localizedName}") }
            }
            is HeaderViewHolder -> {
                sections.getTitle(position)?.let { holder.configure(it) }
            }
        }
    }
}