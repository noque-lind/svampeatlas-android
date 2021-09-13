package com.noque.svampeatlas.view_holders

import android.graphics.Color
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.item_unknown_species.view.*

class UnknownSpeciesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {



    init {

    }


    fun configure(isSelected: Boolean) {
//        if (isSelected) resultsView.setBackgroundColor(Color.TRANSPARENT) else resultsView.setBackgroundColor(itemView.resources.getColor(R.color.colorGreen))
    }
}