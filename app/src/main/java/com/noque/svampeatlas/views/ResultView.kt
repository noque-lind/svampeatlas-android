package com.noque.svampeatlas.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.noque.svampeatlas.extensions.downloadImage
import com.noque.svampeatlas.extensions.italized
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.R
import com.noque.svampeatlas.services.DataService
import kotlinx.android.synthetic.main.view_result.view.*

class ResultView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var imageView: ImageView
    private var primaryLabel: TextView
    private var secondaryLabel: TextView
    private var scoreLabel: TextView


    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_result, this)

        imageView = resultView_imageView
        primaryLabel = resultView_primaryLabel
        secondaryLabel = resultView_secondaryLabel
        scoreLabel = resultView_scoreLabel
    }

    @SuppressLint("DefaultLocale")
    fun configure(mushroom: Mushroom) {
        if (mushroom.isGenus) {
            imageView.setImageResource(R.drawable.icon_genus)
            val primaryText = mushroom.danishName?.capitalize() ?: mushroom.fullName.italized(context)
            primaryLabel.text = resources.getString(R.string.util_genus, primaryText)
            secondaryLabel.text = if (mushroom.danishName != null) mushroom.fullName.italized(context) else null
        } else {
            if (mushroom.images?.firstOrNull() != null) {
                imageView.visibility = View.VISIBLE
                imageView.downloadImage(DataService.ImageSize.MINI, mushroom.images.first().url)
            } else {
                imageView.visibility = View.GONE
            }

            primaryLabel.text = mushroom.danishName ?: mushroom.fullName.italized(context)
            secondaryLabel.text = if (mushroom.danishName != null) mushroom.fullName.italized(context) else null
        }
    }

    fun configure(mushroom: Mushroom, score: Double?) {
        configure(mushroom)

        score?.let {
            scoreLabel.visibility = View.VISIBLE
            scoreLabel.text = "${String.format("%.1f", it * 100)}%"
        }
    }

    fun configure(drawable: Drawable, primaryText: String, secondaryText: String) {
        imageView.visibility = View.VISIBLE
        imageView.setImageDrawable(drawable)

        val params =  LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.CENTER_VERTICAL

        imageView.layoutParams = params
        primaryLabel.text = primaryText
        secondaryLabel.text = secondaryText
    }
}