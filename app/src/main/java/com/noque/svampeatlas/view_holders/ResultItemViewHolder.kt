package com.noque.svampeatlas.view_holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.views.ResultView
import kotlinx.android.synthetic.main.item_result.view.*

class ResultItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val resultView: ResultView = itemView.resultItem_resultView

    fun configure(mushroom: Mushroom) {
       resultView.configure(mushroom)
    }

    fun configure(mushroom: Mushroom, score: Double?) {
        resultView.configure(mushroom, score)
    }
}