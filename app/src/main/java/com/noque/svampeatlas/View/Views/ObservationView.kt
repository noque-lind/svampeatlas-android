package com.noque.svampeatlas.View.Views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.noque.svampeatlas.Extensions.downloadImage
import com.noque.svampeatlas.Model.Image
import com.noque.svampeatlas.Model.Observation
import com.noque.svampeatlas.R
import com.noque.svampeatlas.Services.DataService
import kotlinx.android.synthetic.main.view_add_observation_images.view.*
import kotlinx.android.synthetic.main.view_observation.view.*
import org.w3c.dom.Text

class ObservationView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private lateinit var imageView: ImageView
    private lateinit var primaryTextView: TextView
    private lateinit var secondaryTextView: TextView
    private lateinit var userTextView: TextView


    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_observation, this)
        initViews()
    }

    private fun initViews() {
        imageView = observationView_imageView
        primaryTextView = observationView_primaryTextView
        secondaryTextView = observationView_secondaryTextView
        userTextView = observationView_userTextView
    }

    fun configure(observation: Observation) {
        imageView.visibility = View.GONE

        observation.images.firstOrNull()?.let {
            imageView.downloadImage(DataService.IMAGESIZE.MINI, observation.images.first().url)
            imageView.visibility = View.VISIBLE
        }


        primaryTextView.text = observation.speciesProperties.name
        secondaryTextView.text = observation.location
        userTextView.text = observation.observationBy
    }
}