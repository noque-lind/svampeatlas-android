package com.noque.svampeatlas.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.Item
import com.noque.svampeatlas.models.Section
import com.noque.svampeatlas.models.Sections
import com.noque.svampeatlas.models.ViewType
import com.noque.svampeatlas.view_holders.*

abstract class BaseAdapter<I, V>: RecyclerView.Adapter<RecyclerView.ViewHolder>() where I: Item<V>, V : ViewType, V: Enum<V>  {

    internal var sections = Sections<V, I>()
    abstract val onClickListener: View.OnClickListener

    private var recyclerView: RecyclerView? = null

    open fun setSections(sections: List<Section<I>>) {
        this.sections.setSections(sections)
        notifyDataSetChanged()
    }

    final override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        super.onAttachedToRecyclerView(recyclerView)
    }

    final override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        super.onDetachedFromRecyclerView(recyclerView)
    }

//    fun setSections(sections: Sections<V,I>) {
//        this.sections = sections
//    }

    final override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view: View
        val viewHolder: RecyclerView.ViewHolder
        when (sections.getSectionViewType(viewType)) {
            Section.ViewType.HEADER -> {
                view = layoutInflater.inflate(R.layout.item_header, parent, false)
                viewHolder = HeaderViewHolder(view)
            }
            Section.ViewType.ERROR -> {
                view = layoutInflater.inflate(R.layout.item_error, parent, false)
                viewHolder = ErrorViewHolder(view)
            }
            Section.ViewType.LOADER -> {
                view = layoutInflater.inflate(R.layout.item_loader, parent, false)
                viewHolder = LoaderViewHolder(view)
            }
            Section.ViewType.ITEM -> {
                createViewTypeViewHolder(layoutInflater, parent, viewType - Section.ViewType.values.count()).also {
                    view = it.first
                    viewHolder = it.second
                    view.tag = viewHolder
                    view.setOnClickListener(onClickListener)
                }
            }
        }
        return viewHolder
    }

    @NonNull
    abstract fun createViewTypeViewHolder(inflater: LayoutInflater, parent: ViewGroup, viewTypeOrdinal: Int): Pair<View, RecyclerView.ViewHolder>

    final override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        fun expandUtil(position: Int, itemView: View) {
            if (itemCount == 1 && position == 0) {
                itemView.doOnLayout {
                    recyclerView?.doOnLayout { recyclerView ->
                        val heightDifference = recyclerView.height - it.bottom
                        if (heightDifference > 0) {
                            val layoutParams = it.layoutParams
                            layoutParams.height = it.height + heightDifference
                            it.layoutParams = layoutParams
                        }
                    }
                }
            }
        }

        when (holder) {
            is HeaderViewHolder -> { sections.getTitle(position)?.let { holder.configure(it) } }
            is ErrorViewHolder -> {
                sections.getError(position)?.let { holder.configure(it) }
                expandUtil(position, holder.itemView)
            }
            is ReloaderViewHolder -> { holder.configure(ReloaderViewHolder.Type.LOAD) }
            is LoaderViewHolder -> { expandUtil(position, holder.itemView) }
            else -> bindViewHolder(holder, sections.getItem(position))
        }
    }

    abstract fun bindViewHolder(holder: RecyclerView.ViewHolder, item: I)

    final override fun getItemCount(): Int {
        return sections.getCount()
    }

    final override fun getItemViewType(position: Int): Int {
        return sections.getViewTypeOrdinal(position)
    }
}