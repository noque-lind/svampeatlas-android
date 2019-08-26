package com.noque.svampeatlas.View.Views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Adapters.AddObservationAdapters.AddImagesAdapter
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_add_observation_images.view.*

class AddObservationImagesView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val recyclerView: RecyclerView

    private val adapter by lazy {
        AddImagesAdapter()
    }

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_add_observation_images, this)
        recyclerView = addObservationImagesView_recyclerView
        setupView()
    }

    private fun setupView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }
}