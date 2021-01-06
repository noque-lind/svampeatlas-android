package com.noque.svampeatlas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.view_holders.NewObservationViewHolder
import com.noque.svampeatlas.view_holders.NotificationViewHolder

class NotebookAdapter: BaseAdapter<NotebookAdapter.Items, NotebookAdapter.Items.ViewTypes>() {

    sealed class Items(viewType: ViewTypes) : Item<Items.ViewTypes>(viewType) {
        enum class ViewTypes: ViewType {
            NewObservation
        }

        class Observation(val newObservation: NewObservation): Items(ViewTypes.NewObservation)
    }

    init {
        sections.addSection(Section("Notesbog d. 23/4", State.Loading()))
    }

    override val onClickListener: View.OnClickListener
        get() = TODO("Not yet implemented")

    override fun createViewTypeViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewTypeOrdinal: Int
    ): Pair<View, RecyclerView.ViewHolder> {
        when (Items.ViewTypes.values()[viewTypeOrdinal]) {
            Items.ViewTypes.NewObservation -> {
                val view = inflater.inflate(R.layout.item_observation, parent, false)
                return Pair(
                    view,
                    NewObservationViewHolder(view)
                )

            }
        }
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Item<Items.ViewTypes>) {
        when (item) {
            is Items.Observation -> (holder as? NewObservationViewHolder)?.configure(item.newObservation)
        }
    }
}