package com.noque.svampeatlas.ViewHolders

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
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