package com.noque.svampeatlas.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.Views.MushroomView
import com.noque.svampeatlas.ViewHolders.MushroomViewHolder

import kotlinx.android.synthetic.main.item_mushroom.view.*

class MushroomListAdapter():
    RecyclerView.Adapter<MushroomViewHolder>() {


    private var mushrooms: List<Mushroom> = listOf()
    private var onClickListener: ((mushroom: Mushroom) -> Unit)? = null


    fun updateData(mushrooms: List<Mushroom>) {
        this.mushrooms = mushrooms
        notifyDataSetChanged()
    }

    fun setOnClickListener(listener: (mushroom: Mushroom) -> Unit) {
        onClickListener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MushroomViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_mushroom, parent, false)
        return MushroomViewHolder(onClickListener, view)
    }

    override fun getItemCount(): Int = mushrooms.size

    override fun onBindViewHolder(holder: MushroomViewHolder, position: Int) {
        holder.configure(mushrooms[position])
    }
}