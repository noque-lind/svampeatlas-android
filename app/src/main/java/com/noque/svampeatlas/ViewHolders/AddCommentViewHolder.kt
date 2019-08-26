package com.noque.svampeatlas.ViewHolders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_add_comment.view.*

class AddCommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val editText = itemView.addComment_editText
    private val sendButton = itemView.addComment_sendButton

    fun setOnClickListener(listener: View.OnClickListener) {
        sendButton.setOnClickListener {
            listener.onClick(itemView)
        }
    }

    fun getComment(): String? {
        return if (editText.text.isNullOrEmpty()) null else editText.text.toString()
    }
}