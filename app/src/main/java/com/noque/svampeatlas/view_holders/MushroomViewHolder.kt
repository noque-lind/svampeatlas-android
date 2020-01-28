package com.noque.svampeatlas.view_holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.Mushroom
import kotlinx.android.synthetic.main.item_mushroom.view.*

class MushroomViewHolder(private val itemClick: ((Mushroom) -> Unit)?, var view: View): RecyclerView.ViewHolder(view) {

    private val mushroomView = itemView.item_mushroom_mushroomView

    init {
        mushroomView.round(false)
    }

    fun configure(mushroom: Mushroom) {
        mushroomView.configure(mushroom)
        mushroomView.setOnClickListener {
            itemClick?.invoke(mushroom)
        }
    }
}