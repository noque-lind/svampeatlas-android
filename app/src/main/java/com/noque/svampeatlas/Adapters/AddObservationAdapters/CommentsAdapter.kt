package com.noque.svampeatlas.Adapters.AddObservationAdapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Model.Comment
import com.noque.svampeatlas.R
import com.noque.svampeatlas.ViewHolders.AddCommentViewHolder
import com.noque.svampeatlas.ViewHolders.CommentViewHolder

class CommentsAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class ViewType {
        COMMENT,
        ADDCOMMENT;

        companion object {
            val values = values()
        }
    }


    companion object {
        val TAG = "CommentsAdapter"
    }


    private var canComment: Boolean = false
    private var comments = mutableListOf<Comment>()

    var sendCommentClicked: ((String) -> Unit)? = null

    private val sendCommentButtonClicked = View.OnClickListener {
        (it.tag as? AddCommentViewHolder)?.let {
            it.getComment()?.let {
                sendCommentClicked?.invoke(it)
            }
        }
    }

    fun configure(comments: List<Comment>, canComment: Boolean) {
        this.canComment = canComment
        this.comments = comments.toMutableList()
        notifyDataSetChanged()
    }

    fun addComment(comment: Comment) {
        this.comments.add(comment)
        notifyItemInserted(comments.lastIndex)
    }

    override fun getItemViewType(position: Int): Int {
        return if (position <= comments.lastIndex) ViewType.COMMENT.ordinal else ViewType.ADDCOMMENT.ordinal
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        var view: View
        var viewHolder: RecyclerView.ViewHolder

        when (ViewType.values[viewType]) {
            ViewType.COMMENT -> {
                view = layoutInflater.inflate(R.layout.item_comment, parent, false)
                viewHolder = CommentViewHolder(view)
            }
            ViewType.ADDCOMMENT -> {
                view = layoutInflater.inflate(R.layout.item_add_comment, parent, false)
                val addCommentViewHolder = AddCommentViewHolder(view)
                addCommentViewHolder.setOnClickListener(sendCommentButtonClicked)
                viewHolder = addCommentViewHolder
                view.tag = viewHolder
            }
        }

        return viewHolder
    }

    override fun getItemCount(): Int {
       return if (canComment) comments.count() + 1 else comments.count()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as? CommentViewHolder)?.configure(comments[position])
    }
}