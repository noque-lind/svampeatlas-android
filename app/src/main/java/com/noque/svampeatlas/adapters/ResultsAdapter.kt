package com.noque.svampeatlas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.add_observation.SpeciesAdapter
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.view_holders.*

class ResultsAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun reloadSelected()
        fun predictionResultSelected(predictionResult: PredictionResult)
    }

    sealed class Item(viewType: ViewType) : com.noque.svampeatlas.models.Item<Item.ViewType>(viewType) {
        class Result(val predictionResult: PredictionResult): Item(ViewType.RESULTVIEW)
        class TryAgain(): Item(ViewType.RETRYVIEW)
        class Caution(): Item(ViewType.CAUTIONVIEW)
        class Creditation(): Item(ViewType.CREDITATION)

        enum class ViewType : com.noque.svampeatlas.models.ViewType {
            RESULTVIEW,
            CREDITATION,
            CAUTIONVIEW,
            RETRYVIEW;

            companion object {
                val values = values()
            }
        }
    }

    private val sections = Sections<Item.ViewType, Item>()

    private var listener: Listener? = null

    fun configure(results: List<PredictionResult>) {
        var highestConfidence = 0.0
        results.forEach {
            if (it.score > highestConfidence) {
                highestConfidence = it.score * 100
            }
        }

        if (highestConfidence < 50.0) {
            sections.setSections(mutableListOf(
                Section<Item>(null, State.Items(listOf(Item.Caution()))),
                Section<Item>(null, State.Items(results.map { Item.Result(it) })),
                Section<Item>(null, State.Items(listOf(Item.Creditation()))),
                Section<Item>(null, State.Items(listOf(Item.TryAgain())))
            ))
        } else {
            sections.setSections(mutableListOf(
                Section<Item>(null, State.Items(results.map { Item.Result(it) })),
                Section<Item>(null, State.Items(listOf(Item.Creditation()))),
                Section<Item>(null, State.Items(listOf(Item.TryAgain())))
            ))
        }


        notifyDataSetChanged()
    }

    fun configure(error: AppError) {
        sections.setSections(mutableListOf(
            Section(null, State.Error(error)),
            Section<Item>(null, State.Items(listOf(Item.TryAgain())))
        ))

        notifyDataSetChanged()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    private val onItemClickListener = View.OnClickListener {
        when (val viewHolder = it.tag) {
            is ReloaderViewHolder -> {
                listener?.reloadSelected()
            }
            is ResultItemViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is Item.Result -> {
                        listener?.predictionResultSelected(item.predictionResult)
                    }
                }
            }
        }
    }


    override fun getItemCount(): Int {
        return sections.getCount()
    }

    override fun getItemViewType(position: Int): Int {
        return sections.getViewTypeOrdinal(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
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
                when (Item.ViewType.values[viewType - Section.ViewType.values.count()]) {
                    Item.ViewType.RESULTVIEW -> {
                        view = layoutInflater.inflate(R.layout.item_result, parent, false)
                        viewHolder = ResultItemViewHolder(view)
                    }

                    Item.ViewType.CREDITATION -> {
                        view = layoutInflater.inflate(R.layout.item_creditation, parent, false)
                        viewHolder = CreditationViewHolder(view)
                    }

                    Item.ViewType.RETRYVIEW -> {
                        view = layoutInflater.inflate(R.layout.item_reloader, parent, false)
                        viewHolder = ReloaderViewHolder(view)
                    }

                    Item.ViewType.CAUTIONVIEW -> {
                        view = layoutInflater.inflate(R.layout.item_caution, parent, false)
                        viewHolder = CautionViewHolder(view)
                    }
                }
            }
        }


        view.tag = viewHolder
        view.setOnClickListener(onItemClickListener)
        return viewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ResultItemViewHolder -> {
                sections.getItem(position).let {
                    when (val item = sections.getItem(position)) {
                        is Item.Result -> {
                            holder.configure(item.predictionResult.mushroom)
                        }
                    }
                }
            }
                is ErrorViewHolder -> {
                    sections.getError(position)?.let { holder.configure(it) }
            }
            
            is ReloaderViewHolder -> {
                holder.configure(ReloaderViewHolder.Type.RELOAD)
            }

            is CreditationViewHolder -> {
                holder.configure(CreditationViewHolder.Type.AI)
            }
        }
    }

}