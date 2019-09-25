package com.noque.svampeatlas.view_holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.Mushroom
import kotlinx.android.synthetic.main.item_mushroom.view.*

class MushroomViewHolder(private val itemClick: ((Mushroom) -> Unit)?, var view: View): RecyclerView.ViewHolder(view) {

    fun configure(mushroom: Mushroom) {
        view.item_mushroom_mushroomView.configure(mushroom)
        view.setOnClickListener {
            itemClick?.invoke(mushroom)
        }
    }
}