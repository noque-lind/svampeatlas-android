package com.noque.svampeatlas.view_holders

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_item.view.*

class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val textView: TextView

    init {
        textView = itemView.itemItem_textView
    }


    fun configure(text: String) {
        textView.text = text
    }
}