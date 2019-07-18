package com.noque.svampeatlas.View

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.Extensions.downloadImage
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.R
import com.noque.svampeatlas.Services.DataService
import kotlinx.android.synthetic.main.mushroom_view_layout.view.*

class MushroomView(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    lateinit var informationLinearLayout: LinearLayout

    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.mushroom_view_layout, this)
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
        Log.d("MushroomView", mushroom.images.toString())
        mushroomView_imageView.downloadImage(DataService.IMAGESIZE.MINI, mushroom.images.first().url)



        if (mushroom.danishName != null) {
            mushroomView_primaryLabel.text = mushroom.danishName
            mushroomView_secondaryLabel.visibility = View.VISIBLE
            mushroomView_secondaryLabel.text = mushroom.fullName
        } else {
            mushroomView_primaryLabel.text = mushroom.fullName
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

//        mushroom.updatedAt?.let {
//            information.add(Pair("Sidst opdateret d.:", it.toString()))
//        }
    }
}
