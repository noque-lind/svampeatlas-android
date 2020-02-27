package com.noque.svampeatlas.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.noque.svampeatlas.extensions.downloadImage
import com.noque.svampeatlas.extensions.italized
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.upperCased
import com.noque.svampeatlas.services.DataService
import kotlinx.android.synthetic.main.view_result.view.*

class ResultView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var imageView: ImageView
    private var primaryLabel: TextView
    private var secondaryLabel: TextView
    private var scoreLabel: TextView
    private var toxicityView: LinearLayout


    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_result, this)

        imageView = resultView_imageView
        primaryLabel = resultView_primaryLabel
        secondaryLabel = resultView_secondaryLabel
        scoreLabel = resultView_scoreLabel
        toxicityView = resultView_toxicityView

        setupView()
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

    fun configure(mushroom: Mushroom) {
        if (mushroom.isGenus) {
            imageView.setImageResource(R.drawable.icon_genus)
            val primaryText = mushroom.localizedName?.upperCased() ?: mushroom.fullName.italized()
            primaryLabel.text = resources.getString(R.string.containedResultCell_genus, primaryText)
            secondaryLabel.text = if (mushroom.localizedName != null) mushroom.fullName.italized() else null
        } else {
            if (mushroom.images?.firstOrNull() != null) {
                imageView.visibility = View.VISIBLE
                imageView.downloadImage(DataService.ImageSize.MINI, mushroom.images.first().url)
            } else {
                imageView.visibility = View.GONE
            }

            primaryLabel.text = mushroom.localizedName?.upperCased() ?: mushroom.fullName.italized()
            secondaryLabel.text = if (mushroom.localizedName != null) mushroom.fullName.italized() else null
        }

        if (mushroom.attributes?.isPoisonous == true) {
            toxicityView.visibility = View.VISIBLE
        } else {
            toxicityView.visibility = View.GONE
        }
    }

    fun configure(mushroom: Mushroom, score: Double?) {
        configure(mushroom)

        if (score != null) {
            scoreLabel.visibility = View.VISIBLE
            scoreLabel.text = "${String.format("%.1f", score * 100)}%"
        } else {
            scoreLabel.visibility = View.GONE
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