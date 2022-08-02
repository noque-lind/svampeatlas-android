package com.noque.svampeatlas.view_holders

import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.Locality
import kotlinx.android.synthetic.main.item_locality.view.*

class LocalityViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


    private var button: Button = itemView.localityItem_button
    private var selected = false


    fun setListener(listener: View.OnClickListener) {
        button.tag = this
        button.setOnClickListener(listener)
    }

    fun configure(locality: Locality, selected: Boolean, isLocked: Boolean) {
        button.text = locality.name
        button.isSelected = selected
        if (isLocked) {
            button.setCompoundDrawablesWithIntrinsicBounds(button.resources.getDrawable(R.drawable.glyph_lock, null), null, null, null)
        } else {
            button.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        }
    }
}