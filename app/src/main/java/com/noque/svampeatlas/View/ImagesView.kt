package com.noque.svampeatlas.View

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Adapters.ImagesAdapter
import com.noque.svampeatlas.Model.Image
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_images.view.*

class ImagesView(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private val imagesAdapter = ImagesAdapter(listOf())

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_images, this)
        initViews()
        setupView()
    }

    private fun initViews() {

    }

    private fun setupView() {
        Log.d("ImagesView", "Images View setupView called")


        imagesView_recyclerView.apply {
            val gridLayout = GridLayoutManager(context, 1)
            gridLayout.orientation = RecyclerView.HORIZONTAL
            layoutManager = gridLayout
            adapter = imagesAdapter
            addItemDecoration(PaginatorDecoration(ContextCompat.getColor(context, R.color.colorThird), ContextCompat.getColor(context, R.color.colorWhite)))

        }

        val pageSnap = PagerSnapHelper()
        pageSnap.attachToRecyclerView(imagesView_recyclerView)
    }

    fun configure(images: List<Image>) {
        Log.d("ImagesView", "Images View configure called ${images.toString()}")
        imagesAdapter.configure(images)
    }
}