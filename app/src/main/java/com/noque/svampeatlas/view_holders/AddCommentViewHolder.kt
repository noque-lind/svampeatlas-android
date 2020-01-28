package com.noque.svampeatlas.view_holders

import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
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
            val system = ContextCompat.getSystemService(itemView.context, InputMethodManager::class.java)
            system?.hideSoftInputFromWindow(editText.windowToken, 0)

        return if (editText.text.isNullOrEmpty()) null else editText.text.toString()
    }
}