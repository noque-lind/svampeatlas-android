package com.noque.svampeatlas.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.noque.svampeatlas.R
import com.noque.svampeatlas.views.BlankActivity
import com.noque.svampeatlas.views.ImagesView
import kotlinx.android.synthetic.main.fragment_image.*

class ImageFragment: Fragment() {

    // Objects

    private val args: ImageFragmentArgs by navArgs()

    // Views

    private lateinit var toolbar: Toolbar
    private lateinit var imagesView: ImagesView

    // Adapters


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
        imagesView.configure(args.images.toList(), args.selectedIndex, ImageView.ScaleType.FIT_CENTER)

    }

    override fun onResume() {
        super.onResume()
        (requireActivity() as BlankActivity).hideSystemBars()
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as BlankActivity).hideSystemBars()
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as BlankActivity).showSystemBars()
    }

    private fun initViews() {
        toolbar = imageFragment_toolbar
        imagesView = imageFragment_imagesView
    }

    private fun setupViews() {
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
    }
}