package com.noque.svampeatlas.adapters.add_observation.details_picker

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.Host
import com.noque.svampeatlas.models.Section
import com.noque.svampeatlas.models.Substrate
import com.noque.svampeatlas.view_holders.ItemViewHolder

class HostsAdapter() : PickerAdapter<Host>() {

    private var selectedHosts: MutableList<Host> = mutableListOf()

    override val onClickListener = View.OnClickListener {
        when (val viewHolder = it.tag) {
            is ItemViewHolder -> {
                val item = sections.getItem(viewHolder.adapterPosition)
                if (it.isSelected) {
                    it.isSelected = false
                    listener?.itemDeselected(item.item)
                    selectedHosts.remove(item.item)
                } else {
                    it.isSelected = true
                    listener?.itemSelected(item.item)
                    selectedHosts.add(item.item)
                }
            }
        }
    }

    fun configure(sections: List<Section<PickerItem<Host>>>, selectedHosts: List<Host>) {
        this.selectedHosts = selectedHosts.toMutableList()
        configure(sections)
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: PickerItem<Host>) {
        holder.itemView.isSelected = selectedHosts.contains(item.item)
        (holder as? ItemViewHolder)?.let { if (item.item.localizedName != null) it.configure("- ${item.item.localizedName} (${item.item.latinName})") else it.configure(item.item.latinName) }
    }
}

