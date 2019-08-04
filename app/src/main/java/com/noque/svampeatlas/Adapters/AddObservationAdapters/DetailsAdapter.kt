package com.noque.svampeatlas.Adapters.AddObservationAdapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.Fragments.AddObservationFragments.DetailsFragment
import com.noque.svampeatlas.ViewHolders.SettingsViewHolder

class DetailsAdapter(private val categories: Array<DetailsFragment.Categories>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun onViewClicked(view: View)
    }

    private var listener: Listener? = null



    private val onClickListener = object: View.OnClickListener {
        override fun onClick(view: View) {
            listener?.onViewClicked(view)
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }


    override fun getItemViewType(position: Int): Int {
        return categories[position].ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        var view: View
        var viewHolder: RecyclerView.ViewHolder
        val layoutInflater = LayoutInflater.from(parent.context)


        when (categories[viewType]) {
            DetailsFragment.Categories.DATE,
            DetailsFragment.Categories.SUBSTRATE,
            DetailsFragment.Categories.HOST,
            DetailsFragment.Categories.VEGETATIONTYPE -> {
                view = layoutInflater.inflate(R.layout.item_setting, parent, false)
                view.setOnClickListener(onClickListener)
                viewHolder = SettingsViewHolder(view)
            }
        }

        return viewHolder
    }

    override fun getItemCount(): Int {
        return categories.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (categories[position]) {
            DetailsFragment.Categories.DATE -> (holder as? SettingsViewHolder)?.configure("Dato:", "")
            DetailsFragment.Categories.SUBSTRATE -> (holder as? SettingsViewHolder)?.configure("Substrat::", "")
            DetailsFragment.Categories.HOST -> (holder as? SettingsViewHolder)?.configure("Vært:", "")
            DetailsFragment.Categories.VEGETATIONTYPE -> (holder as? SettingsViewHolder)?.configure("Vegetationstype:", "")
        }
    }
}