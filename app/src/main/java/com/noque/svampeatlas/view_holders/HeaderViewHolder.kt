package com.noque.svampeatlas.view_holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_header.view.*

class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val headerView = itemView.headerItem_headerView

    fun configure(title: String) {
        headerView.configure(title)
    }
}