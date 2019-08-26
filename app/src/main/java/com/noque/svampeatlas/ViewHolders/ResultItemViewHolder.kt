package com.noque.svampeatlas.ViewHolders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Extensions.downloadImage
import com.noque.svampeatlas.Extensions.italized
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Services.DataService
import com.noque.svampeatlas.View.Views.ResultView
import kotlinx.android.synthetic.main.item_result.view.*
import kotlinx.android.synthetic.main.view_mushroom.view.*

class ResultItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val resultView: ResultView

    init {
        resultView = itemView.resultItem_resultView
    }

    fun configure(mushroom: Mushroom) {
       resultView.configure(mushroom)
    }
}