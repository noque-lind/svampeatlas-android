package com.noque.svampeatlas.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.model.LatLng

import com.noque.svampeatlas.R
import com.noque.svampeatlas.views.BlankActivity
import kotlinx.android.synthetic.main.fragment_observation_location.*

/**
 * A simple [Fragment] subclass.
 */
class ObservationLocationFragment : Fragment() {


    // Objects

    private val args:  ObservationLocationFragmentArgs by navArgs()


    // Views
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var mapFragment: MapFragment



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_observation_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()

        val latLng = LatLng(args.latitude.toDouble(), args.longitude.toDouble())
        mapFragment.addLocationMarker(latLng, getString(R.string.localityFragment_sightingLocation))
        mapFragment.setRegion(latLng, 8000)
    }

    private fun initViews() {
        mapFragment = childFragmentManager.findFragmentById(R.id.observationLocationFragment_mapFragment) as MapFragment
        toolbar = observationLocationFragment_toolbar
    }

    private fun setupViews() {
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
        mapFragment.showStyleSelector(true)
    }
}
