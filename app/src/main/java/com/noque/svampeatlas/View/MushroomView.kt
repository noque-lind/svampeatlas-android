package com.noque.svampeatlas.View

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.Extensions.downloadImage
import com.noque.svampeatlas.Extensions.italized
import com.noque.svampeatlas.Extensions.upperCased
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.R
import com.noque.svampeatlas.Services.DataService
import com.noque.svampeatlas.View.Views.InformationView
import kotlinx.android.synthetic.main.view_mushroom.view.*

class MushroomView(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    lateinit var informationLinearLayout: LinearLayout

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_mushroom, this)
        initViews()
        setupView()
    }

    private fun initViews() {
        informationLinearLayout = LinearLayout(context)
        informationLinearLayout.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                informationLinearLayout.orientation = LinearLayout.VERTICAL
    }

    private fun setupView() {
        outlineProvider = object: ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                val radius = 45F

                view?.let {
                    outline?.setRoundRect(0, 0, (view.width + radius).toInt(), view.height, radius)
                }
            }
        }

        mushroomView_linearLayout.addView(informationLinearLayout)

        clipToOutline = true
    }

    fun configure(mushroom: Mushroom) {
        if (mushroom.images.firstOrNull() != null) {
            mushroomView_imageView.downloadImage(DataService.IMAGESIZE.MINI, mushroom.images.first().url)
        }

        if (mushroom.danishName != null) {
            mushroomView_primaryLabel.text = mushroom.danishName!!.upperCased()
            mushroomView_secondaryLabel.visibility = View.VISIBLE
            mushroomView_secondaryLabel.text = mushroom.fullName.italized(context)
        } else {
            mushroomView_primaryLabel.text = mushroom.fullName.italized(context)
            mushroomView_secondaryLabel.visibility = View.GONE
        }

        var information: MutableList<Pair<String, String>> = mutableListOf()

        mushroom.totalObservations?.let {
            information.add(Pair("Antal danske fund:", it.toString()))
        }

        mushroom.lastAcceptedObservation?.let {
            information.add(Pair("Seneste fund:", it.toString()))
        }

        mushroomView_informationView.configure(information)
    }
}
