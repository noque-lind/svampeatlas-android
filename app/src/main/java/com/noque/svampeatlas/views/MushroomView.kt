package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.noque.svampeatlas.extensions.downloadImage
import com.noque.svampeatlas.extensions.italized
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.extensions.upperCased
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.R
import com.noque.svampeatlas.services.DataService
import kotlinx.android.synthetic.main.view_mushroom.view.*

class MushroomView(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    interface Listener {
        fun onClicked(mushroom: Mushroom)
    }


    private var mushroom: Mushroom? = null
    private var listener: Listener? = null

    private lateinit var informationLinearLayout: LinearLayout

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
        mushroomView_linearLayout.addView(informationLinearLayout)
        clipToOutline = true
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
        this.setOnClickListener {
            mushroom?.let { listener?.onClicked(it) }
        }
    }

    fun round(fully: Boolean) {
        outlineProvider = object: ViewOutlineProvider() {
            override fun getOutline(view: View?, outline: Outline?) {
                val radius = 45F

                view?.let {
                    if (fully) {
                        outline?.setRoundRect(0,0,view.width, view.height, radius)
                    } else {
                        outline?.setRoundRect(0, 0, (view.width + radius).toInt(), view.height, radius)
                    }
                }
            }
        }
    }

    fun configure(mushroom: Mushroom) {
        this.mushroom = mushroom

        if (!mushroom.images.isNullOrEmpty()) {
            mushroomView_imageView.visibility = View.VISIBLE
            mushroomView_imageView.downloadImage(DataService.ImageSize.MINI, mushroom.images.first().url)
        } else {
            mushroomView_imageView.visibility = View.GONE
        }

        if (mushroom.danishName != null) {
            mushroomView_primaryLabel.text = mushroom.danishName!!.upperCased()
            mushroomView_secondaryLabel.visibility = View.VISIBLE
            mushroomView_secondaryLabel.text = mushroom.fullName.italized()
        } else {
            mushroomView_primaryLabel.text = mushroom.fullName.italized()
            mushroomView_secondaryLabel.visibility = View.GONE
        }

        var information: MutableList<Pair<String, String>> = mutableListOf()

        mushroom.statistics?.acceptedObservationsCount?.let {
            information.add(Pair("Antal danske fund:", it.toString()))
        }

        mushroom.statistics?.lastAcceptedObservationDate?.let {
            information.add(Pair("Seneste fund:", it.toReadableDate(true, true)))
        }

        mushroomView_informationView.configure(information)
    }
}
