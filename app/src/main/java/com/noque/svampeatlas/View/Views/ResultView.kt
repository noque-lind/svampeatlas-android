package com.noque.svampeatlas.View.Views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.noque.svampeatlas.Extensions.downloadImage
import com.noque.svampeatlas.Extensions.italized
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.R
import com.noque.svampeatlas.Services.DataService
import kotlinx.android.synthetic.main.view_result.view.*

class ResultView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var imageView: ImageView
    private var primaryLabel: TextView
    private var secondaryLabel: TextView


    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_result, this)

        imageView = resultView_imageView
        primaryLabel = resultView_primaryLabel
        secondaryLabel = resultView_secondaryLabel
    }

    fun configure(mushroom: Mushroom) {
        if (mushroom.images.firstOrNull() != null) {
            imageView.visibility = View.VISIBLE
            imageView.downloadImage(DataService.IMAGESIZE.MINI, mushroom.images.first().url)
        } else {
            imageView.visibility = View.GONE
        }

        primaryLabel.text = mushroom.danishName ?: mushroom.fullName.italized(context)
        secondaryLabel.text = if (mushroom.danishName != null) mushroom.fullName.italized(context) else null
    }
}