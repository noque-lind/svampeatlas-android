package com.noque.svampeatlas.views

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.italized
import com.noque.svampeatlas.extensions.upperCased
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.Observation
import kotlinx.android.synthetic.main.view_mushroom_header.view.*
import kotlinx.android.synthetic.main.view_observation_header.view.*

class MushroomDetailsHeaderView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_mushroom_header, this)
    }

    fun configure(mushroom: Mushroom) {
        mushroomHeaderView_titleTextView.text =
            mushroom.localizedName ?: mushroom.fullName.italized()
        if (mushroom.localizedName != null) mushroomHeaderView_subtitleTextView.text =
            mushroom.fullName.italized() else mushroomHeaderView_subtitleTextView.visibility =
            View.GONE

        if (mushroom.redListStatus != null) {
            mushroomHeaderView_redlistShortIcon.text = mushroom.redListStatus
            when (mushroom.redListStatus) {
                "LC", "NT" -> {
                    mushroomHeaderView_redlistShortIcon.setBackgroundResource(R.drawable.circle_view_color_green)
                    mushroomHeaderView_redlistLabel.text =
                        resources.getString(R.string.redlistView_lcnt)
                }
                "CR", "EN"-> {
                    mushroomHeaderView_redlistShortIcon.setBackgroundResource(R.drawable.circle_view_color_red)
                    mushroomHeaderView_redlistLabel.text =
                        resources.getString(R.string.redlistView_lcnt)
                }
                "VU" -> {
                    mushroomHeaderView_redlistShortIcon.setBackgroundResource(R.drawable.circle_view_color_yellow)
                    mushroomHeaderView_redlistLabel.text =
                        resources.getString(R.string.redlistView_vu)
                }
                "DD" -> {
                    mushroomHeaderView_redlistShortIcon.setBackgroundResource(R.drawable.circle_view_color_secondary)
                    mushroomHeaderView_redlistLabel.text =
                        resources.getString(R.string.redlistView_dd)
                }
            }
        } else {
            mushroomHeaderView_redlistLabel.visibility = View.GONE
            mushroomHeaderView_redlistShortIcon.visibility = View.GONE
    }
    }
}