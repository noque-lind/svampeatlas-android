package com.noque.svampeatlas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.Comment
import com.noque.svampeatlas.R
import com.noque.svampeatlas.view_holders.AddCommentViewHolder
import com.noque.svampeatlas.view_holders.CommentViewHolder

class CommentsAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun sendComment(comment: String)
    }


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

    private var listener: Listener? = null

    private val sendCommentButtonClicked = View.OnClickListener {
        (it.tag as? AddCommentViewHolder)?.let {
            it.getComment()?.let {
                listener?.sendComment(it)
            }
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun configure(comments: List<Comment>?, canComment: Boolean) {
        if (this.comments != comments || this.comments.isEmpty()) {
            this.canComment = canComment
            comments?.let { this.comments = it.toMutableList() }
            notifyDataSetChanged()
        }
    }

    fun addComment(comment: Comment) {
        this.comments.add(comment)
        notifyItemInserted(comments.lastIndex)
        notifyItemChanged(comments.lastIndex + 1)
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