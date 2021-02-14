package com.noque.svampeatlas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.fragments.modals.DownloaderFragment
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.view_holders.DownloadTaxonViewHolder
import com.noque.svampeatlas.view_holders.NewObservationViewHolder
import com.noque.svampeatlas.view_holders.NotificationViewHolder

class NotebookAdapter: BaseAdapter<NotebookAdapter.Items, NotebookAdapter.Items.ViewTypes>() {

    interface Listener {
        fun newObservationSelected(newObservation: NewObservation)
        fun downloadForOfflinePressed()
    }


    sealed class Items(viewType: ViewTypes) : Item<Items.ViewTypes>(viewType) {
        enum class ViewTypes: ViewType {
            DownloadPrompt,
            NewObservation
        }

        class Note(val newObservation: NewObservation): Items(ViewTypes.NewObservation)
        class DownloadPrompt : Items(ViewTypes.DownloadPrompt)
    }

    override fun setSections(sections: List<Section<Items>>) {
        if (SharedPreferences.lastDownloadOfTaxon == null) {
            val finalList = mutableListOf(Section.Builder<Items>().items(listOf(Items.DownloadPrompt())).build())
            finalList.addAll(sections)
            super.setSections(finalList)
        } else {
            super.setSections(sections)
        }
    }



    var listener: Listener? = null

    override val onClickListener: View.OnClickListener
        get() = View.OnClickListener {
            when (val tag = it.tag) {
                is NewObservationViewHolder -> {
                    (sections.getItem(tag.adapterPosition) as? Items.Note)?.let { listener?.newObservationSelected(it.newObservation) }
                }
                is DownloadTaxonViewHolder -> {
                    listener?.downloadForOfflinePressed()
                }
            }
        }

    override fun createViewTypeViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewTypeOrdinal: Int
    ): Pair<View, RecyclerView.ViewHolder> {
        when (Items.ViewTypes.values()[viewTypeOrdinal]) {
            Items.ViewTypes.NewObservation -> {
                val view = inflater.inflate(R.layout.item_new_observation, parent, false)
                return Pair(
                    view,
                    NewObservationViewHolder(view)
                )

            }
            Items.ViewTypes.DownloadPrompt -> {
                val view = inflater.inflate(R.layout.item_download_taxon, parent, false)
                return Pair(view, DownloadTaxonViewHolder(view))
            }
        }
    }

    override fun bindViewHolder(holder: RecyclerView.ViewHolder, item: Items) {
        when (holder) {
            is NewObservationViewHolder -> (item as? Items.Note)?.let { holder.configure(item.newObservation) }
        }
    }
}