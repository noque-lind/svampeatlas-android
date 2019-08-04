package com.noque.svampeatlas.Adapters.AddObservationAdapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Model.Observation
import com.noque.svampeatlas.R
import com.noque.svampeatlas.ViewHolders.HeaderViewHolder
import kotlinx.android.synthetic.main.item_header.view.*

class AddObservationSpecieAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class Section(val title: String) {

        enum class ViewType(val value: Int) {
            UNKNOWNSPECIE(0),
            SELECTEDSPECIE(1),
            SELECTABLE(2),
            HEADER(3);

            companion object {
                fun fromInt(int: Int): ViewType {
                    when (int) {
                        0 -> return UNKNOWNSPECIE
                        1 -> return SELECTEDSPECIE
                        2 -> return SELECTABLE
                        else -> return HEADER
                    }
                }
            }
        }


        fun getType(): ViewType {
            when (this) {
                is UnknownSpecie -> return ViewType.UNKNOWNSPECIE
                is SelectedMushroom -> return ViewType.SELECTEDSPECIE
                is SelectableMushroom -> return ViewType.SELECTABLE
            }
        }

        fun getCount(): Int {
            when (this) {
                is UnknownSpecie -> return 2
                is SelectableMushroom -> return this.mushrooms.count() + 1
                is SelectedMushroom -> return 2
            }
        }

        fun getMushroom(position: Int): Mushroom? {
            when (this) {
                is UnknownSpecie -> return null
                is SelectableMushroom -> return this.mushrooms.getOrNull(position)
                is SelectedMushroom -> return this.mushroom
            }
        }


        class UnknownSpecie(title: String): Section(title)
        class SelectableMushroom(title: String, val mushrooms: List<Mushroom>): Section(title)
        class SelectedMushroom(title: String, val mushroom: Mushroom) : Section(title)
    }



    private var sections = listOf<Section>()

    fun configure(sections: List<Section>) {
        this.sections = sections
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        var count = 0

        sections.forEach {
            count += it.getCount()
        }

        return count
    }


    override fun getItemViewType(position: Int): Int {
        var currentPosition = 0

        sections.forEach {

            if (position >= currentPosition && position <= (currentPosition + it.getCount() - 1)) {
                if (position == currentPosition) {
                    return Section.ViewType.HEADER.value
                } else {
                    return it.getType().value
                }
            }

            currentPosition += it.getCount()
        }


        sections[position]
        return super.getItemViewType(position)
    }

    fun getSection(position: Int): Section? {
        var currentPosition = 0

        sections.forEach {
            if (position == currentPosition && position <= (currentPosition + it.getCount() - 1)) {
                return it
            }
            currentPosition += it.getCount()
        }
        return null
    }

    fun getItem(position: Int): Mushroom? {
        var currentPosition = 0

            sections.forEach {
                if (position >= currentPosition && position <= (currentPosition + it.getCount() - 1)) {
                    return it.getMushroom(currentPosition + position)
                }
                currentPosition += it.getCount()
            }

        return null
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val layoutInflater = LayoutInflater.from(parent.context)
        var view: View
        var viewHolder: RecyclerView.ViewHolder

         val type =
             Section.ViewType.fromInt(
                 viewType
             )

        Log.d("AddObservat", type.toString())

        when (type) {
            Section.ViewType.HEADER -> {
                view = layoutInflater.inflate(R.layout.item_header, parent, false)
                viewHolder = HeaderViewHolder(view)
            }

            Section.ViewType.SELECTABLE -> {
                view = layoutInflater.inflate(R.layout.view_result, parent, false)
                viewHolder = ViewHolderSelectable(view)
            }

            else -> {
                view = layoutInflater.inflate(R.layout.nav_header, parent, false)
                viewHolder = throwAway(view)
            }
        }

        return viewHolder
    }




    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (Section.ViewType.fromInt(
            holder.itemViewType
        )) {
            Section.ViewType.SELECTABLE -> {
                (holder as? ViewHolderSelectable)?.configure(getItem(position))
            }

            Section.ViewType.HEADER -> {
                (holder as? HeaderViewHolder)?.configure(getSection(position)?.title ?: "Header")
            }
            else -> {
            }
        }
    }
}


class ViewHolderSelectable(itemView: View) : RecyclerView.ViewHolder(itemView) {

    fun configure(mushroom: Mushroom?) {
        Log.d("View holde", mushroom.toString())
    }


    fun configure(observation: Observation) {

    }
}


class throwAway(itemView: View) : RecyclerView.ViewHolder(itemView)