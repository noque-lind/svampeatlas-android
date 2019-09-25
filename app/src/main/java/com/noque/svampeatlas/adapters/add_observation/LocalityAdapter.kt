package com.noque.svampeatlas.adapters.add_observation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.Locality
import com.noque.svampeatlas.R
import com.noque.svampeatlas.view_holders.LocalityViewHolder

class LocalityAdapter(): RecyclerView.Adapter<LocalityViewHolder>() {

    private var localities = listOf<Locality>()
    private var selectedPosition = 0
    var localitySelected: ((locality: Locality) -> Unit)? = null


    private val onClickListener = View.OnClickListener {
        (it.tag as? RecyclerView.ViewHolder)?.let {
            localitySelected?.invoke(localities[it.adapterPosition])
        }
    }

    fun configure(localities: List<Locality>) {
        this.localities = localities
        notifyDataSetChanged()
    }

    fun setSelected(locality: Locality): Int {
        notifyItemChanged(selectedPosition)
        selectedPosition = localities.indexOf(locality)
        notifyItemChanged(selectedPosition)
        return selectedPosition
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocalityViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_locality, parent, false)
        val localityViewHolder = LocalityViewHolder(view)
        localityViewHolder.setListener(onClickListener)
        return localityViewHolder
    }

    override fun getItemCount(): Int {
        return localities.count()
    }

    override fun onBindViewHolder(holder: LocalityViewHolder, position: Int) {
       holder.configure(localities[position], position == selectedPosition)
    }
}