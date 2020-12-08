package com.noque.svampeatlas.views

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.models.Observation
import kotlinx.android.synthetic.main.view_observation_header.view.*
import kotlinx.android.synthetic.main.view_mushroom_header.view.*

class ObservationHeaderView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    interface Listener {
        fun menuButtonPressed(view: View)
    }

    private var listener: Listener? = null

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_observation_header, this)
        observationHeaderView_moreButton.setOnClickListener {
            listener?.menuButtonPressed(it)
        }
    }

    fun configure(observation: Observation, listener: Listener) {
        this.listener = listener
        observationHeaderView_idLabel.text = "DMS: ${observation.id} | ${observation.observationBy} | ${observation.observationDate?.toReadableDate(true, true)}"
        observationHeaderView_titleLabel.text = observation.determination.localizedName ?: observation.determination.fullName

        when (observation.validationStatus) {
            Observation.ValidationStatus.APPROVED -> {
                observationHeaderView_determinationIcon.setImageResource(R.drawable.glyph_checkmark)
                observationHeaderView_determinationIcon.setBackgroundResource(R.drawable.circle_view_color_green)
                observationHeaderView_determinationLabel.text = resources.getString(R.string.determinationLabel_approved)
            }
            Observation.ValidationStatus.VERIFYING -> {
                observationHeaderView_determinationIcon.setImageResource(R.drawable.glyph_neutral)
                observationHeaderView_determinationIcon.setBackgroundResource(R.drawable.circle_view_color_primary)
                observationHeaderView_determinationLabel.text = resources.getString(R.string.determinationLabel_validating)
            }
            Observation.ValidationStatus.REJECTED -> {
                observationHeaderView_determinationIcon.setImageResource(R.drawable.glyph_denied)
                observationHeaderView_determinationIcon.setBackgroundResource(R.drawable.circle_view_color_red)
                observationHeaderView_determinationLabel.text = resources.getString(R.string.determinationLabel_rejected)
            }
            Observation.ValidationStatus.UNKNOWN -> {
                observationHeaderView_determinationIcon.visibility = View.GONE
                observationHeaderView_determinationLabel.visibility = View.GONE
            }
        }
    }
}