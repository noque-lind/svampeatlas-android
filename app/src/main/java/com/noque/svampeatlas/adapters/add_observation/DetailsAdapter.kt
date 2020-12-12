package com.noque.svampeatlas.adapters.add_observation

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.models.Host
import com.noque.svampeatlas.models.Substrate
import com.noque.svampeatlas.models.VegetationType
import com.noque.svampeatlas.R
import com.noque.svampeatlas.fragments.add_observation.DetailsFragment
import com.noque.svampeatlas.view_holders.InputTypeViewHolder
import com.noque.svampeatlas.view_holders.SettingsViewHolder
import java.util.*

class DetailsAdapter(private val resources: Resources, private val categories: Array<DetailsFragment.Categories>) :
    RecyclerView.Adapter<ViewHolder>() {

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

    private val onClickListener = View.OnClickListener { view ->
        (view.tag as? ViewHolder)?.adapterPosition?.let {
            categoryClicked?.invoke(categories[it])
        }
    }

    private val onInputTypeChanged: ((view: View, text: String?) -> Unit) = { view, text ->
        (view.tag as? ViewHolder)?.adapterPosition?.let {
            when (categories[it]) {
                DetailsFragment.Categories.ECOLOGYNOTES -> ecologyNotes = text
                DetailsFragment.Categories.NOTES -> notes = text
                else -> {}
            }
            onTextInputChanged?.invoke(categories[it], text)
        }
    }


    override fun getItemViewType(position: Int): Int {
        return categories[position].ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view: View
        val viewHolder: ViewHolder
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
                resources.getString(R.string.observationDetailsCell_date),
                date?.toReadableDate(recentFormatting = false, ignoreTime = true) ?: "-"
            )
            DetailsFragment.Categories.SUBSTRATE -> {
                val string = if (substrate?.second == true) "\uD83D\uDD12 " else ""

                (holder as? SettingsViewHolder)?.configure(
                    R.drawable.glyph_substrate,
                    resources.getString(R.string.observationDetailsCell_substrate), string.plus(substrate?.first?.localizedName ?: "*")
                )
            }

            DetailsFragment.Categories.VEGETATIONTYPE -> {
                val string = if (vegetationType?.second == true) "\uD83D\uDD12 " else ""
            (holder as? SettingsViewHolder)?.configure(
                R.drawable.glyph_vegetation_type,
                resources.getString(R.string.observationDetailsCell_vegetationType),
                string.plus(vegetationType?.first?.localizedName ?: "*")
            )

        }

            DetailsFragment.Categories.HOST -> {
                var hostsString: String


                if (hosts?.first.isNullOrEmpty()) {
                    hostsString = "-"
                } else {
                    hostsString = if (hosts?.second == true) "\uD83D\uDD12 " else ""
                    hosts?.first?.forEach {
                        hostsString += if (it.localizedName != null) "${it.localizedName}, " else "${it.latinName}, "
                    }
                   hostsString = hostsString.dropLast(2)

                }

                (holder as? SettingsViewHolder)?.configure(R.drawable.glyph_host,resources.getString(R.string.observationDetailsCell_host), hostsString)
            }


            DetailsFragment.Categories.ECOLOGYNOTES -> (holder as? InputTypeViewHolder)?.configure(
                resources.getString(R.string.observationDetailsCell_ecologyNotes_title),
                resources.getString(R.string.observationDetailsCell_ecologyNotes_message),
                ecologyNotes
            )

            DetailsFragment.Categories.NOTES -> (holder as? InputTypeViewHolder)?.configure(
                resources.getString(R.string.observationDetailsCell_notes_title),
                resources.getString(R.string.observationDetailsCell_notes_message),
                notes
            )
        }
    }
}