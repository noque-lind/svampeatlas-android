package com.noque.svampeatlas.adapters.add_observation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.view_holders.*
import com.noque.svampeatlas.view_models.NewObservationViewModel

class SpeciesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun mushroomSelected(mushroom: Mushroom)
        fun confidenceSet(confidence: NewObservationViewModel.DeterminationConfidence)
    }

    companion object {
        val TAG = "SpeciesAdapter"
        val DEFAULT_MUSHROOM =
            Mushroom(60212, "Fungi Sp.", null, null, 0, null, null, null, null, null, null)
    }

    sealed class Item(viewType: ViewType) :
        com.noque.svampeatlas.models.Item<Item.ViewType>(viewType) {
        class UnknownSpecies: Item(ViewType.UNKNOWNSPECIES)
        class SelectableMushroom(val mushroom: Mushroom, val score: Double? = null) : Item(ViewType.SELECTABLE)
        class SelectedMushroom(
            val mushroom: Mushroom,
            val confidence: NewObservationViewModel.DeterminationConfidence
        ) : Item(ViewType.SELECTEDSPECIES)

        enum class ViewType : com.noque.svampeatlas.models.ViewType {
            UNKNOWNSPECIES,
            SELECTEDSPECIES,
            SELECTABLE;

            companion object {
                val values = values()
            }
        }
    }

    private val sections = Sections<Item.ViewType, Item>()

    private val upperSection = Section<Item>(null)
    private val middleSection = Section<Item>(null)
    private val suggestionsSection = Section<Item>(null)

    private var listener: Listener? = null

    private val onClickListener = View.OnClickListener { view ->
        when (val viewHolder = view.tag) {
            is SelectedResultItemViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is Item.SelectedMushroom -> {
                        listener?.mushroomSelected(item.mushroom)
                    }
                }
            }
            is UnknownSpeciesViewHolder -> {
                listener?.mushroomSelected(DEFAULT_MUSHROOM)
            }
            is ResultItemViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is Item.SelectableMushroom -> {
                        listener?.mushroomSelected(item.mushroom)
                    }
                }
            }
        }
    }

    init {
        sections.addSection(upperSection)
        sections.addSection(middleSection)
        sections.addSection(suggestionsSection)
    }


    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun configureUpperSection(state: State<List<Item>>, title: String?) {
        upperSection.setTitle(title)
        upperSection.setState(state)
        notifyDataSetChanged()
    }

    fun configureMiddleSectionState(state: State<List<Item>>, title: String?) {
        middleSection.setTitle(title)
        middleSection.setState(state)
        notifyDataSetChanged()
    }

    fun configureSuggestionsSection(state: State<List<Item>>, title: String?) {
        suggestionsSection.setTitle(title)
        suggestionsSection.setState(state)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return sections.getCount()
    }

    override fun getItemViewType(position: Int): Int {
        return sections.getViewTypeOrdinal(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        var view: View
        var viewHolder: RecyclerView.ViewHolder

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
                    Item.ViewType.UNKNOWNSPECIES -> {
                        view = layoutInflater.inflate(R.layout.item_unknown_species, parent, false)
                        view.setOnClickListener(onClickListener)
                        viewHolder = UnknownSpeciesViewHolder(view)
                    }
                    Item.ViewType.SELECTEDSPECIES -> {
                        view = layoutInflater.inflate(R.layout.item_selected_result, parent, false)
                        val selectedResultItemViewHolder = SelectedResultItemViewHolder(view)
                        selectedResultItemViewHolder.confidenceSet = {
                            listener?.confidenceSet(it)
                        }

                        selectedResultItemViewHolder.setOnClickListener(onClickListener)
                        viewHolder = selectedResultItemViewHolder
                    }

                    Item.ViewType.SELECTABLE -> {
                        view = layoutInflater.inflate(R.layout.item_result, parent, false)
                        view.setOnClickListener(onClickListener)
                        viewHolder = ResultItemViewHolder(view)
                    }
                }
            }
        }

        view.tag = viewHolder
        return viewHolder
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                sections.getTitle(position)?.let { holder.configure(it) }
            }

            is ResultItemViewHolder -> {
                when (val item = sections.getItem(position)) {
                    is Item.SelectableMushroom -> {
                        holder.configure(item.mushroom, item.score)
                    }
                }
            }

            is SelectedResultItemViewHolder -> {
                when (val item = sections.getItem(position)) {
                    is Item.SelectedMushroom -> {
                        holder.configure(item.mushroom, item.confidence)
                    }
                }
            }

            is ErrorViewHolder -> {
                sections.getError(position)?.let { holder.configure(it) }
            }
        }
    }
}