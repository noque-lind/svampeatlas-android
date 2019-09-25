package com.noque.svampeatlas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.R
import com.noque.svampeatlas.view_holders.ObservationViewHolder
import com.noque.svampeatlas.view_holders.ReloaderViewHolder

class ObservationsAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private enum class ViewType {
        RESULTSITEM,
        RELOADERITEM;

        companion object {
            val values = values()
        }
    }

    private var isPaging = false
    private var observations = listOf<Observation>()

    var observationClicked: ((Observation) -> Unit)? = null
    var addtionalDataAtOffsetRequested: ((Int) -> Unit)? = null

    private val onClickListener = View.OnClickListener {
        (it.tag as? ObservationViewHolder)?.let {
            observationClicked?.invoke(observations[it.adapterPosition])
        }

        (it.tag as? ReloaderViewHolder)?.let {
            addtionalDataAtOffsetRequested?.invoke(it.adapterPosition)
        }
    }

    fun configure(observations: List<Observation>, isPaging: Boolean) {
        this.isPaging = isPaging
        this.observations = observations
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position <= observations.lastIndex) {
            ViewType.RESULTSITEM.ordinal
        } else {
            return ViewType.RELOADERITEM.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: View
        val viewHolder: RecyclerView.ViewHolder

       when (ViewType.values[viewType]) {
           ViewType.RESULTSITEM -> {
               view = inflater.inflate(R.layout.item_observation, parent, false)
               viewHolder = ObservationViewHolder(view)
           }
           ViewType.RELOADERITEM -> {
               view = inflater.inflate(R.layout.item_reloader, parent, false)
               viewHolder = ReloaderViewHolder(view)
           }
       }

        viewHolder.itemView.tag = viewHolder
        viewHolder.itemView.setOnClickListener(onClickListener)
        return viewHolder
    }

    override fun getItemCount(): Int {
        return if (isPaging) observations.count() + 1 else observations.count()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? ObservationViewHolder)?.let {
            it.configure(observations[position])
        }
    }
}