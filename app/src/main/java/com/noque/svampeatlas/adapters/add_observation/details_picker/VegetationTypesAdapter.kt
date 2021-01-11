package com.noque.svampeatlas.adapters.add_observation.details_picker

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.VegetationType
import com.noque.svampeatlas.view_holders.ItemViewHolder

class VegetationTypesAdapter: PickerAdapter<VegetationType>() {
    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: PickerItem<VegetationType>) {
        (holder as? ItemViewHolder)?.configure("- ${item.item.localizedName}")
    }
}