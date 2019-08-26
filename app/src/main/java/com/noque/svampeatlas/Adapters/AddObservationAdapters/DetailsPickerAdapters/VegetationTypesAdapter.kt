package com.noque.svampeatlas.Adapters.AddObservationAdapters.DetailsPickerAdapters

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Adapters.PickerAdapter
import com.noque.svampeatlas.Model.VegetationType
import com.noque.svampeatlas.ViewHolders.ItemViewHolder

class VegetationTypesAdapter: PickerAdapter<VegetationType>() {

    private var selectedItem: Int = RecyclerView.NO_POSITION

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position)?.let { vegetationType ->
            (holder as? ItemViewHolder)?.configure("- ${vegetationType.dkName}")
        }
        super.onBindViewHolder(holder, position)
    }
}