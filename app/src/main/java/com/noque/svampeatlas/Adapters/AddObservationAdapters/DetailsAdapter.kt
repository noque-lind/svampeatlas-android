package com.noque.svampeatlas.Adapters.AddObservationAdapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.noque.svampeatlas.Extensions.upperCased
import com.noque.svampeatlas.Model.Host
import com.noque.svampeatlas.Model.Substrate
import com.noque.svampeatlas.Model.VegetationType
import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.Fragments.AddObservationFragments.DetailsFragment
import com.noque.svampeatlas.ViewHolders.InputTypeViewHolder
import com.noque.svampeatlas.ViewHolders.SettingsViewHolder
import java.util.*

class DetailsAdapter(private val categories: Array<DetailsFragment.Categories>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var date: Date? = null
    var substrate: Pair<Substrate, Boolean>? = null
    var vegetationType: Pair<VegetationType, Boolean>? = null
    var hosts: Pair<List<Host>, Boolean>? = null
    var notes: String? = null
    var ecologyNotes: String? = null

    fun updateCategory(category: DetailsFragment.Categories) {
        notifyItemChanged(category.ordinal)
    }

    var categoryClicked: ((category: DetailsFragment.Categories) -> Unit)? = null
    var onTextInputChanged: ((category: DetailsFragment.Categories, text: String?) -> Unit)? = null

    private val onClickListener = object: View.OnClickListener {
        override fun onClick(view: View) {
            (view.tag as? RecyclerView.ViewHolder)?.adapterPosition?.let {
                categoryClicked?.invoke(categories[it])
            }
        }
    }

    private val onInputTypeChanged: ((view: View, text: String?) -> Unit) = { view, text ->
        (view.tag as? ViewHolder)?.adapterPosition?.let {
            onTextInputChanged?.invoke(categories[it], text)
        }
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
            DetailsFragment.Categories.NOTES,
            DetailsFragment.Categories.ECOLOGYNOTES -> {
                view = layoutInflater.inflate(R.layout.item_text_input, parent, false)
                viewHolder = InputTypeViewHolder(onInputTypeChanged, view)
            }
        }

        viewHolder.itemView.tag = viewHolder
        return viewHolder
    }

    override fun getItemCount(): Int {
        return categories.count()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (categories[position]) {
            DetailsFragment.Categories.DATE -> (holder as? SettingsViewHolder)?.configure(
                R.drawable.glyph_age,
                "Dato:",
                date.toString() ?: "-"
            )
            DetailsFragment.Categories.SUBSTRATE -> {
                val string = if (substrate?.second == true) "\uD83D\uDD12 " else ""

                (holder as? SettingsViewHolder)?.configure(
                    R.drawable.glyph_substrate,
                    "Substrat:", string.plus(substrate?.first?.dkName ?: "*")
                )
            }

            DetailsFragment.Categories.VEGETATIONTYPE -> {
                val string = if (vegetationType?.second == true) "\uD83D\uDD12 " else ""
            (holder as? SettingsViewHolder)?.configure(
                R.drawable.glyph_vegetation_type,
                "Vegetationstype:",
                string.plus(vegetationType?.first?.dkName ?: "*")
            )

        }

            DetailsFragment.Categories.HOST -> {
                var hostString = if (hosts?.second == true) "\uD83D\uDD12 " else ""
                hosts?.let {
                    it.first.forEach { hostString += "${it.dkName}, " }
                    hostString = hostString.dropLast(2)
                } ?: run {
                    hostString = "-"
                }

                (holder as? SettingsViewHolder)?.configure(R.drawable.glyph_host,"Vært:", hostString)
            }


            DetailsFragment.Categories.ECOLOGYNOTES -> (holder as? InputTypeViewHolder)?.configure(
                "Økologi noter",
                ecologyNotes
            )

            DetailsFragment.Categories.NOTES -> (holder as? InputTypeViewHolder)?.configure(
                "Andre noter",
                notes
            )
        }
    }
}