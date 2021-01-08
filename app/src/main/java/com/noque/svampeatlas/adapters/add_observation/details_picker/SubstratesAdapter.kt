package com.noque.svampeatlas.adapters.add_observation.details_picker

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.Substrate
import com.noque.svampeatlas.view_holders.HeaderViewHolder
import com.noque.svampeatlas.view_holders.ItemViewHolder

class SubstratesAdapter(): PickerAdapter<Substrate>() {
    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: PickerItem<Substrate>) {
        (holder as? ItemViewHolder)?.configure("- ${item.item.localizedName}")
    }
}