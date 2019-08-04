package com.noque.svampeatlas.ViewHolders

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_setting.view.*

class SettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var iconImageView: ImageView
    private var titleTextView: TextView
    private var contentTextView: TextView

    init {
        iconImageView = itemView.settingItem_iconImageView
        titleTextView = itemView.settingItem_titleTextView
        contentTextView = itemView.settingItem_contentTextView
    }


    fun configure(title: String, content: String?) {
        titleTextView.text = title
        contentTextView.text = content
    }

}