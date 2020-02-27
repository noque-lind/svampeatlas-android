package com.noque.svampeatlas.view_holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.item_reloader.view.*

class ReloaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    enum class Type {
        RELOAD,
        LOAD
    }

    private val textView = itemView.reloaderItem_textView

    fun configure(type: Type) {
        when (type) {
            Type.RELOAD -> { textView.setText(R.string.reloadCell_tryAgain) }
            Type.LOAD -> { textView.setText(R.string.reloadCell_showMore) }
        }
    }
}