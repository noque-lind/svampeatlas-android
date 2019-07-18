package com.noque.svampeatlas.View


import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.toSpannable
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.noque.svampeatlas.Extensions.italized
import com.noque.svampeatlas.Extensions.upperCased
import com.noque.svampeatlas.Model.Image
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.ViewModel.DetailsViewModel
import kotlinx.android.synthetic.main.fragment_details.*
import kotlinx.android.synthetic.main.mushroom_view_layout.view.*
import com.noque.svampeatlas.R



class DetailsFragment : Fragment() {

    lateinit var detailsViewModel: DetailsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            detailsViewModel = ViewModelProviders.of(it).get(DetailsViewModel::class.java)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        arguments?.let {
//            images = DetailsFragmentArgs.fromBundle(it).images
//            configureView()
//        }

        detailsViewModel.selected.observe(viewLifecycleOwner, Observer {
            configureView(it)
        })
    }

    override fun onResume() {
        super.onResume()

//        if i want custom toolbar
//        val toolbar = binding.toolbar
//        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        (activity as AppCompatActivity).let {
            it.supportActionBar?.hide()
        }
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity).let {
            it.supportActionBar?.show()
        }
    }

    fun configureView(mushroom: Mushroom) {
        fragmentDetails_imagesView.configure(mushroom.images)
        fragmentDetails_titlesView.configure(mushroom.danishName?.upperCased()?:mushroom.fullName.italized(), (if (mushroom.danishName == null) null else mushroom.fullName.italized()))
        addDescriptionView("Beskrivelse", mushroom.attributes?.diagnosis?.capitalize())
        addDescriptionView("Forvekslingsmuligheder", mushroom.attributes?.similarities?.capitalize())

        var information: MutableList<Pair<String, String>> = mutableListOf()

        mushroom.totalObservations?.let {
            information.add(Pair("Antal danske fund:", it.toString()))
        }

        mushroom.lastAcceptedObservation?.let {
            information.add(Pair("Seneste fund:", it))
        }

        mushroom.updatedAt?.let {
            information.add(Pair("Sidst opdateret d.:", it))
        }

        if (information.size != 0) {
                val informationView = InformationView(context, null)
                informationView.configure(information)

                val contentView = ContentView(context, null)
                contentView.configure("Informationer", informationView)

                fragmentDetails_linearLayout.addView(contentView)
            }


        context?.let {
//            animalImage.loadImage(animal.imageURL, getProgressDrawable(it))
        }
    }

    fun addDescriptionView(title: String?, content: String?) {
        if (content != null && content != "") {
            val descriptionView = DescriptionView(context, null)
            descriptionView.configure(title, content)
            fragmentDetails_descriptionViewLinearLayout.addView(descriptionView)

            val space = Space(context)
            space.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (24F * (context?.getResources()?.displayMetrics?.density ?: 1F)).toInt())

            fragmentDetails_descriptionViewLinearLayout.addView(space)
        }
    }
}
