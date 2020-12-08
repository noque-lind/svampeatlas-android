package com.noque.svampeatlas.adapters.add_observation.details_picker

import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Header
import com.noque.svampeatlas.adapters.PickerAdapter
import com.noque.svampeatlas.models.Host
import com.noque.svampeatlas.view_holders.HeaderViewHolder
import com.noque.svampeatlas.view_holders.ItemViewHolder

class HostsAdapter() : PickerAdapter<Host>() {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ItemViewHolder -> {
                sections.getItem(position)
                    .item.let { if (it.localizedName != null) holder.configure("- ${it.localizedName} (${it.latinName})") else holder.configure(it.latinName) }
            }
        }
        super.onBindViewHolder(holder, position)
    }
}