package com.noque.svampeatlas.adapters.add_observation.details_picker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.Section
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.BaseAdapter
import com.noque.svampeatlas.models.Item
import com.noque.svampeatlas.models.Sections
import com.noque.svampeatlas.models.ViewType
import com.noque.svampeatlas.view_holders.HeaderViewHolder
import com.noque.svampeatlas.view_holders.ItemViewHolder

abstract class PickerAdapter<T>() : BaseAdapter<PickerAdapter.PickerItem<T>, PickerAdapter.PickerItem.ViewTypes>() {

    interface Listener<T> {
        fun itemSelected(item: T)
        fun itemDeselected(item: T)
    }

    class PickerItem<T>(val item: T) : Item<PickerItem.ViewTypes>(
        ViewTypes.ITEM
    ) {
        enum class ViewTypes : ViewType {
            ITEM
        }
    }

    internal var listener: Listener<T>? = null

    override val onClickListener = View.OnClickListener {
        when (val viewHolder = it.tag) {
            is ItemViewHolder -> {
                it.isSelected = true
                listener?.itemSelected(sections.getItem(viewHolder.adapterPosition).item)
            }
        }
    }

    fun setListener(listener: Listener<T>) {
        this.listener = listener
    }

    fun configure(
        sections: List<Section<PickerItem<T>>>
    ) {
        this.sections.setSections(sections.toMutableList())
        notifyDataSetChanged()
    }

    override fun createViewTypeViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewTypeOrdinal: Int
    ): Pair<View, RecyclerView.ViewHolder> {
        inflater.inflate(R.layout.item_item,parent,false).apply {
            return Pair(this, ItemViewHolder(this))
        }
    }
}