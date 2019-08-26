package com.noque.svampeatlas.Adapters.AddObservationAdapters.DetailsPickerAdapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Adapters.PickerAdapter
import com.noque.svampeatlas.Model.Host
import com.noque.svampeatlas.ViewHolders.ItemViewHolder

class HostsAdapter() : PickerAdapter<Host>() {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position)?.let { host ->
            (holder as? ItemViewHolder)?.configure("- ${host.dkName} (${host.latinName})")
        }

        super.onBindViewHolder(holder, position)
    }
}