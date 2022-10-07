package com.noque.svampeatlas.view_holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_title.view.*

class TitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val titleView = itemView.titleItem_title
    private val messageView = itemView.titleItem_message

    fun configure(title: Int, message: Int) {
        titleView.text = itemView.resources.getString(title)
        messageView.text = itemView.resources.getString(message)
    }
}