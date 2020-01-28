package com.noque.svampeatlas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Header
import com.noque.svampeatlas.models.Section
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.Item
import com.noque.svampeatlas.models.Sections
import com.noque.svampeatlas.models.ViewType
import com.noque.svampeatlas.view_holders.HeaderViewHolder
import com.noque.svampeatlas.view_holders.ItemViewHolder

open class PickerAdapter<T>() : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener<T> {
        fun itemSelected(item: T)
        fun itemDeselected(item: T)
    }

    class Item<T>(val item: T) :
        com.noque.svampeatlas.models.Item<Item.ViewType>(Item.ViewType.ITEM) {
        enum class ViewType : com.noque.svampeatlas.models.ViewType {
            ITEM
        }
    }

    private var listener: Listener<T>? = null

    private var selectedPositions: MutableList<Int> = mutableListOf()


    internal val sections = Sections<Item.ViewType, Item<T>>()

    private val onClickListener = View.OnClickListener {
        when (val viewHolder = it.tag) {
            is ItemViewHolder -> {
                if (selectedPositions.contains(viewHolder.adapterPosition)) {
                    it.isSelected = false
                    listener?.itemDeselected(sections.getItem(viewHolder.adapterPosition).item)
                    selectedPositions.remove(viewHolder.adapterPosition)
                } else {
                    it.isSelected = true
                    listener?.itemSelected(sections.getItem(viewHolder.adapterPosition).item)
                    selectedPositions.add(viewHolder.adapterPosition)
                }
            }
        }
    }

    fun setListener(listener: Listener<T>) {
        this.listener = listener
    }


    fun configure(
        sections: List<Section<Item<T>>>,
        selectedPositions: MutableList<Int>? = null
    ) {
        selectedPositions?.let { this.selectedPositions = it }
        sections.forEach { this.sections.addSection(it) }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return sections.getCount()
    }

    override fun getItemViewType(position: Int): Int {
        return sections.getViewTypeOrdinal(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view: View
        val viewHolder: RecyclerView.ViewHolder

        when (sections.getSectionViewType(viewType)) {
            Section.ViewType.HEADER -> {
                view = layoutInflater.inflate(R.layout.item_header, parent, false)
                viewHolder = HeaderViewHolder(view)
            }
            Section.ViewType.ERROR -> TODO()
            Section.ViewType.LOADER -> TODO()
            Section.ViewType.ITEM -> {
                view = layoutInflater.inflate(R.layout.item_item, parent, false)
                view.setOnClickListener(onClickListener)
                viewHolder = ItemViewHolder(view)
                view.tag = viewHolder
            }

        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.isSelected = selectedPositions.contains(position)
    }
}


//
//    internal fun getSection(position: Int): Section<T>? {
//        var currentPosition = 0
//
//        sections.forEach {
//            if (position == currentPosition) {
//                return it
//            }
//            currentPosition += it.getCount()
//        }
//        return null
//    }
//
//   internal fun getItem(position: Int): T? {
//        var currentPosition = 0
//
//        sections.forEach {
//            if (position >= currentPosition && position <= (currentPosition + it.getCount() - 1)) {
//                return it.getItem(position - currentPosition)
//            }
//            currentPosition += it.getCount()
//        }
//        return null
//    }
//
//    override fun getItemViewType(position: Int): Int {
//        var currentPosition = 0
//
//        sections.forEach {
//            if (position >= currentPosition && position <= (currentPosition + it.getCount() - 1)) {
//                return it.getViewType(position - currentPosition).ordinal
//            }
//            currentPosition += it.getCount()
//
//        }
//        return super.getItemViewType(position)
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
//        val layoutInflater = LayoutInflater.from(parent.context)
//        var view: View
//        var viewHolder: RecyclerView.ViewHolder
//
//        when (Section.ViewType.values[viewType]) {
//            Section.ViewType.ITEM -> {
//                view = layoutInflater.inflate(R.layout.item_item, parent, false)
//                viewHolder = ItemViewHolder(view)
//
//                view.setOnClickListener {
//                    val adapterPosition = viewHolder.adapterPosition
//
//                    if (selectedPositions.contains(adapterPosition))  {
//                        it.isSelected = false
//                        getItem(adapterPosition)?.let {itemDeSelected?.invoke(it)}
//                        selectedPositions.remove(adapterPosition)
//                    } else {
//                        it.isSelected = true
//                        getItem(adapterPosition)?.let {itemSelected?.invoke(it)}
//                        selectedPositions.add(adapterPosition)
//                    }
//                }
//            }
//            Section.ViewType.HEADER -> {
//                view = layoutInflater.inflate(R.layout.item_header, parent, false)
//                viewHolder = HeaderViewHolder(view)
//            }
//            else -> { throw IndexOutOfBoundsException() }
//        }
//        return viewHolder
//    }
//
//    override fun getItemCount(): Int {
//        var count = 0
//
//        sections.forEach {
//            count += it.getCount()
//        }
//
//        return count
//    }
//
//    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
//        holder.itemView.isSelected = selectedPositions.contains(position)
//    }