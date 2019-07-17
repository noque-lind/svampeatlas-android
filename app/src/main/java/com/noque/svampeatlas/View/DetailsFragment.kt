package com.noque.svampeatlas.View


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.noque.svampeatlas.Model.Image
import com.noque.svampeatlas.Model.Mushroom

import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.fragment_details.*

class DetailsFragment : Fragment() {

    lateinit var images: Array<Image>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            images = DetailsFragmentArgs.fromBundle(it).images
            configureView()
        }
    }

    override fun onResume() {
        super.onResume()

//        if i want custom toolbar
//        val toolbar = binding.toolbar
//        (activity as AppCompatActivity).setSupportActionBar(toolbar)

//        (activity as AppCompatActivity).let {
//            it.supportActionBar?.hide()
//        }
    }

    override fun onStop() {
        super.onStop()
        (activity as AppCompatActivity).let {
            it.supportActionBar?.show()
        }
    }

    fun configureView() {
         fragmentDetails_imagesView.configure(images.toList())

//        animalLocation.text = animal.location
//        animalName.text = animal.name
//        animalLifespan.text = animal.lifeSpan
//        animalDiet.text = animal.diet

        context?.let {
//            animalImage.loadImage(animal.imageURL, getProgressDrawable(it))
        }
    }

}
