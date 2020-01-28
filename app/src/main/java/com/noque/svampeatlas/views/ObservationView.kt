package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.noque.svampeatlas.extensions.downloadImage
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.extensions.upperCased
import com.noque.svampeatlas.models.Image
import com.noque.svampeatlas.services.DataService
import kotlinx.android.synthetic.main.view_observation.view.*

class ObservationView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var _observation: Observation? = null
    val observation: Observation? get() = _observation

    private lateinit var imageView: ImageView
    private lateinit var primaryTextView: TextView
    private lateinit var secondaryTextView: TextView
    private lateinit var userTextView: TextView
    private lateinit var validationStatusImageView: ImageView


    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_observation, this)
        initViews()
        setupView()
    }

    private fun initViews() {
        imageView = observationView_imageView
        primaryTextView = observationView_primaryTextView
        secondaryTextView = observationView_secondaryTextView
        userTextView = observationView_userTextView
        validationStatusImageView = observationView_validationImageView
    }

    private fun setupView() {
        imageView.clipToOutline = true
        imageView.outlineProvider = object: ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                view?.let {
                    val radius = resources.getDimension(R.dimen.app_rounded_corners)
                    outline?.setRoundRect(0,0,view.width, view.height, radius)
                }
            }
        }
    }

    fun configure(observation: Observation, showValidationStatus: Boolean = false) {
        this._observation = observation

        imageView.visibility = View.GONE

        observation.images.firstOrNull()?.let {
            imageView.downloadImage(DataService.ImageSize.MINI, observation.images.first().url)
            imageView.visibility = View.VISIBLE
        }

        if (showValidationStatus) {
            validationStatusImageView.visibility = View.VISIBLE

            when (observation.validationStatus) {
                Observation.ValidationStatus.APPROVED -> {
                    validationStatusImageView.setImageResource(R.drawable.glyph_checkmark)
                    validationStatusImageView.setBackgroundResource(R.drawable.circle_view_color_green)
                }
                Observation.ValidationStatus.VERIFYING -> {
                    validationStatusImageView.setImageResource(R.drawable.glyph_neutral)
                    validationStatusImageView.setBackgroundResource(R.drawable.circle_view_color_primary)
                }
                Observation.ValidationStatus.REJECTED -> {
                    validationStatusImageView.setImageResource(R.drawable.glyph_denied)
                    validationStatusImageView.setBackgroundResource(R.drawable.circle_view_color_red)
                }
                Observation.ValidationStatus.UNKNOWN -> {
                    validationStatusImageView.visibility = View.GONE
                }
            }
        }

        primaryTextView.text = observation.speciesProperties.name.upperCased()

        val date = observation.date
        if (date != null) {
            secondaryTextView.text = "${date.toReadableDate(true, true)}, ${observation.location}"
        } else {
            secondaryTextView.text = observation.location
        }

        userTextView.text = observation.observationBy
    }
}