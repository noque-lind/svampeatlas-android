package com.noque.svampeatlas.Adapters.AddObservationAdapters.DetailsPickerAdapters

import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Adapters.PickerAdapter
import com.noque.svampeatlas.Model.Substrate
import com.noque.svampeatlas.ViewHolders.HeaderViewHolder
import com.noque.svampeatlas.ViewHolders.ItemViewHolder

class SubstratesAdapter(): PickerAdapter<Substrate>() {

    private var selectedItem: Int = RecyclerView.NO_POSITION

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        getItem(position)?.let { substrate ->
            (holder as? ItemViewHolder)?.configure("- ${substrate.dkName}")
        }

        (holder as? HeaderViewHolder)?.configure(getSection(position)?.title() ?: "Header failure")

        super.onBindViewHolder(holder, position)
    }
}