package com.noque.svampeatlas.Adapters.AddObservationAdapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Model.Observation
import com.noque.svampeatlas.Model.Section
import com.noque.svampeatlas.R
import com.noque.svampeatlas.ViewHolders.HeaderViewHolder
import com.noque.svampeatlas.ViewHolders.ItemViewHolder
import com.noque.svampeatlas.ViewHolders.ResultItemViewHolder
import com.noque.svampeatlas.ViewHolders.SelectedResultItemViewHolder
import com.noque.svampeatlas.ViewModel.NewObservationViewModel
import kotlinx.android.synthetic.main.item_header.view.*
import retrofit2.http.Header

class SpeciesAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    data class Item(val viewType: Item.ViewType, val mushroom: Mushroom?, val confidence: NewObservationViewModel.DeterminationConfidence? = null) {
        enum class ViewType {
            UNKOWNSPECIE,
            SELECTEDSPECIE,
            SELECTABLE;

            companion object {
                val values = values()
            }
        }
    }


    var mushroomSelected: ((Mushroom) -> Unit)? = null
    var confidenceSet: ((NewObservationViewModel.DeterminationConfidence) -> Unit)? = null

    private var sections = listOf<Section<Item>>()
    private val onClickListener = View.OnClickListener { view ->
        (view.tag as? RecyclerView.ViewHolder)?.adapterPosition?.let {
            getItem(it)?.let {
                it.mushroom?.let {
                    mushroomSelected?.invoke(it)
                }
            }
        }
    }


    fun configure(sections: List<Section<Item>>) {
        this.sections = sections
        notifyDataSetChanged()

        if (sections.firstOrNull()?.getItem(0)?.viewType != Item.ViewType.SELECTEDSPECIE) {

        }
    }

    private fun getSection(position: Int): Section<Item>? {
        var currentPosition = 0

        sections.forEach {
            if (position == currentPosition) {
                return it
            }
            currentPosition += it.count()
        }
        return null
    }

    private fun getItem(position: Int): Item? {
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
                val viewType = it.viewType(position - currentPosition)
               return when (viewType) {
                    Section.ViewType.HEADER -> {  viewType.ordinal }
                    Section.ViewType.ITEM -> { (getItem(position)?.viewType?.ordinal)?.plus(1) ?: viewType.ordinal }
                }
            }
            currentPosition += it.count()

        }
        return super.getItemViewType(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        var view: View
        var viewHolder: RecyclerView.ViewHolder

        if (viewType >= 1) {
            when (Item.ViewType.values[viewType - 1]) {
                Item.ViewType.UNKOWNSPECIE -> {
                    Log.d("ADAPTEr", "UNKNOWN SPECIES")
                    view = layoutInflater.inflate(R.layout.item_item, parent, false)
                    viewHolder = ResultItemViewHolder(view)
                }

                Item.ViewType.SELECTABLE -> {
                    Log.d("ADAPTEr", "SELECTABLE")
                    view = layoutInflater.inflate(R.layout.item_result, parent, false)
                    viewHolder = ResultItemViewHolder(view)
                    viewHolder.itemView.tag = viewHolder
                    viewHolder.itemView.setOnClickListener(onClickListener)
                }

                Item.ViewType.SELECTEDSPECIE -> {
                    view = layoutInflater.inflate(R.layout.item_selected_result, parent, false)
                    val selectedResultItemViewHolder = SelectedResultItemViewHolder(view)
                    selectedResultItemViewHolder.confidenceSet = confidenceSet
                    selectedResultItemViewHolder.setOnClickListener(onClickListener)
                    viewHolder = selectedResultItemViewHolder
                    viewHolder.itemView.setOnClickListener(onClickListener)
                }
            }
        } else {
            view = layoutInflater.inflate(R.layout.item_header, parent, false)
            viewHolder = HeaderViewHolder(view)
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
            if (holder.itemViewType == 0) {
                (holder as? HeaderViewHolder)?.configure(getSection(position)?.title() ?: "")
            } else {
                getItem(position)?.let {
                    when (it.viewType) {
                        Item.ViewType.SELECTABLE -> { (holder as? ResultItemViewHolder)?.configure(it.mushroom!!) }
                        Item.ViewType.SELECTEDSPECIE -> { (holder as? SelectedResultItemViewHolder)?.configure(it.mushroom!!, it.confidence) }
                    }
                    Log.d("Adapter", it.mushroom?.toString())
                }
            }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        mushroomSelected = null
        confidenceSet = null
        super.onDetachedFromRecyclerView(recyclerView)
    }
}