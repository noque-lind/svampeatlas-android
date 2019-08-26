package com.noque.svampeatlas.ViewHolders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Model.AppError
import kotlinx.android.synthetic.main.item_error.view.*

class ErrorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val titleTextView = itemView.errorItem_title
    private val secondaryTextView = itemView.errorItem_secondary

    fun configure(error: AppError) {
        titleTextView.text = error.title
        secondaryTextView.text = error.message
    }
}