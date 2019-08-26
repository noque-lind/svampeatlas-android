package com.noque.svampeatlas.ViewHolders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Model.Comment
import kotlinx.android.synthetic.main.item_comment.view.*

class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var profileImageView = itemView.commentItem_profileImageView
    private var nameTextView = itemView.commentItem_nameTextView
    private var dateTextView = itemView.commentItem_dateTextView
    private var contentTextview = itemView.commentItem_contentTextView


    fun configure(comment: Comment) {
        nameTextView.text = comment.commenterName
        contentTextview.text = comment.content
        dateTextView.text = comment.date
        profileImageView.configure(comment.initials, comment.commenterProfileImageURL)
    }
}