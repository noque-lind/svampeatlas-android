package com.noque.svampeatlas.Adapters.AddObservationAdapters

import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Model.Substrate
import com.noque.svampeatlas.Model.VegetationType
import com.noque.svampeatlas.R
import com.noque.svampeatlas.ViewHolders.HeaderViewHolder
import com.noque.svampeatlas.ViewHolders.ItemViewHolder
import retrofit2.http.Header

class Section<T>(private val title: String?, private val items: List<T>) {
    enum class ViewType {
        HEADER,
        ITEM;

        companion object {
            val values: List<ViewType> = values().toList()
        }
    }

    fun count(): Int {
        return if (title != null) items.count() + 1 else items.count()
    }

    fun viewType(position: Int): ViewType {
        return if (position == 0) ViewType.HEADER else ViewType.ITEM
    }

    fun title(): String? {
        return title
    }

    fun getItem(position: Int): T? {
        if (position == 0) {
            return null
        }


        Log.d("Adapter", position.toString())
        return if (title != null) items[position - 1] else items[position]
    }
}

open class PickerAdapter<T>(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var sections = listOf<Section<T>>()

    fun configure(sections: List<Section<T>>) {
        this.sections = sections
        notifyDataSetChanged()
    }

    fun getSection(position: Int): Section<T>? {
        var currentPosition = 0

        sections.forEach {
            if (position == currentPosition) {
                return it
            }
            currentPosition += it.count()
        }
        return null
    }

    fun getItem(position: Int): T? {
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
}

class SubstrateAdapater(): PickerAdapter<Substrate>() {
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        Log.d("SubstrateAdap", getItem(position).toString())

        (holder as? ItemViewHolder)?.configure(getItem(position)?.dkName ?: "Failure")
        (holder as? HeaderViewHolder)?.configure(getSection(position)?.title() ?: "Header failure")

//        when (Section.ViewType.values[holder.itemViewType]) {
//            Section.ViewType.HEADER -> (holder as? ItemViewHolder)?.configure(getSection(position)?.getTitle()?: "Header")
//        }
//
//        sections.forEach {
//            if (position >= currentPosition && position <= (currentPosition + it.getCount() - 1)) {
//                return it.getViewType(currentPosition + position).ordinal
//            }
//
//            currentPosition += it.getCount()
//        }
//
//
//
//        (holder as? ItemViewHolder)?.configure((sections[0] as Section.VegetationTypes).vegetationTypes[position].dkName)
//        super.onBindViewHolder(holder, position)
    }

}


class SubstratesAdapter(): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var sections = listOf<Section>()

    sealed class Section {
        class VegetationTypes(val vegetationTypes: List<VegetationType>): Section()
        class Substrates(val _title: String?, val substrates: List<Substrate>): Section()


        enum class ViewType {
            HEADER,
            ITEM;

            companion object {
                val values: List<ViewType> = ViewType.values().toList()
            }
        }

        fun getCount(): Int {
            when (this) {
                is VegetationTypes -> return this.vegetationTypes.count()
                is Substrates -> return if (_title != null) (substrates.count() + 1) else substrates.count()
            }
        }

        fun getViewType(position: Int): ViewType {
            when (this) {
                is VegetationTypes -> return ViewType.ITEM
                is Substrates -> return if (position == 0) ViewType.HEADER else ViewType.ITEM
            }
        }

        fun getTitle(): String? {
            when (this) {
                is Substrates -> return _title
            }
            return null
        }
    }


    fun configure(sections: List<Section>) {
        this.sections = sections
        notifyDataSetChanged()
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
//                return it.getMushroom(currentPosition + position)
            }
            currentPosition += it.getCount()
        }

        return null
    }

    override fun getItemViewType(position: Int): Int {
        var currentPosition = 0

        sections.forEach {
            if (position >= currentPosition && position <= (currentPosition + it.getCount() - 1)) {
               return it.getViewType(currentPosition + position).ordinal
            }

            currentPosition += it.getCount()
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
            count += it.getCount()
        }

        return count
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var currentPosition = 0

        when (Section.ViewType.values[holder.itemViewType]) {
           Section.ViewType.HEADER -> (holder as? ItemViewHolder)?.configure(getSection(position)?.getTitle()?: "Header")
        }

        sections.forEach {
            if (position >= currentPosition && position <= (currentPosition + it.getCount() - 1)) {
//                return it.getViewType(currentPosition + position).ordinal
            }

            currentPosition += it.getCount()
        }



        (holder as? ItemViewHolder)?.configure((sections[0] as Section.VegetationTypes).vegetationTypes[position].dkName)
    }
}