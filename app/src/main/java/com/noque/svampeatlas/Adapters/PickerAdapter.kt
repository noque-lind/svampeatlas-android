package com.noque.svampeatlas.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Model.Section
import com.noque.svampeatlas.R
import com.noque.svampeatlas.ViewHolders.HeaderViewHolder
import com.noque.svampeatlas.ViewHolders.ItemViewHolder

open class PickerAdapter<T>(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedPositions: MutableList<Int> = mutableListOf()

    var itemSelected: ((item: T) -> Unit)? = null
    var itemDeSelected: ((item: T) -> Unit)? = null

    private var sections = listOf<Section<T>>()

    fun configure(sections: List<Section<T>>, selectedPositions: MutableList<Int>? = null) {
        selectedPositions?.let { this.selectedPositions = it }
        this.sections = sections
        notifyDataSetChanged()
    }

    internal fun getSection(position: Int): Section<T>? {
        var currentPosition = 0

        sections.forEach {
            if (position == currentPosition) {
                return it
            }
            currentPosition += it.count()
        }
        return null
    }

   internal fun getItem(position: Int): T? {
        var currentPosition = 0

        sections.forEach {
            if (position >= currentPosition && position <= (currentPosition + it.count() - 1)) {
                return it.getItem(position - currentPosition)
            }
            currentPosition += it.count()
        }
        return null
    }

    override fun getItemViewType(position: Int): Int {
        var currentPosition = 0

        sections.forEach {
            if (position >= currentPosition && position <= (currentPosition + it.count() - 1)) {
                return it.viewType(position - currentPosition).ordinal
            }
            currentPosition += it.count()

        }
        return super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        var view: View
        var viewHolder: RecyclerView.ViewHolder

        when (Section.ViewType.values[viewType]) {
            Section.ViewType.ITEM -> {
                view = layoutInflater.inflate(R.layout.item_item, parent, false)
                viewHolder = ItemViewHolder(view)

                view.setOnClickListener {
                    val adapterPosition = viewHolder.adapterPosition

                    if (selectedPositions.contains(adapterPosition))  {
                        it.isSelected = false
                        getItem(adapterPosition)?.let {itemDeSelected?.invoke(it)}
                        selectedPositions.remove(adapterPosition)
                    } else {
                        it.isSelected = true
                        getItem(adapterPosition)?.let {itemSelected?.invoke(it)}
                        selectedPositions.add(adapterPosition)
                    }
                }
            }
            Section.ViewType.HEADER -> {
                view = layoutInflater.inflate(R.layout.item_header, parent, false)
                viewHolder = HeaderViewHolder(view)
            }
        }
        return viewHolder
    }

    override fun getItemCount(): Int {
        var count = 0

        sections.forEach {
            count += it.count()
        }

        return count
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        holder.itemView.isSelected = selectedPositions.contains(position)
    }
}