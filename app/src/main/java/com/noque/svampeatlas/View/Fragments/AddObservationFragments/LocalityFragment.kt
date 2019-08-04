package com.noque.svampeatlas.View.Fragments.AddObservationFragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.noque.svampeatlas.Model.Locality
import com.noque.svampeatlas.Model.State
import com.noque.svampeatlas.R
import com.noque.svampeatlas.Services.LocationService
import com.noque.svampeatlas.View.Fragments.MapFragment
import com.noque.svampeatlas.View.Fragments.MapFragmentDelegate
import com.noque.svampeatlas.ViewModel.NewObservationViewModel
import kotlinx.android.synthetic.main.fragment_locality.*

class LocalityFragment: Fragment() {

    lateinit var mapFragment: MapFragment
    lateinit var viewModel: NewObservationViewModel

    private val localityStateObserver = object: Observer<State<List<Locality>>> {
        override fun onChanged(t: State<List<Locality>>) {
            when (t) {
                is State.Items ->  {

                    mapFragment.addLocalities(t.items)
                    mapFragment.zoomToShowMarkers()
                }
            }
        }

    }

    private val localityObserver = object: Observer<Locality> {
        override fun onChanged(locality: Locality?) {

        }
    }

    private val mapFragmentListener = object: MapFragmentDelegate {
        override fun localityPicked(locality: Locality) {
            viewModel.setLocality(locality)
        }

        override fun mapReady() {
            viewModel.localityState.observe(viewLifecycleOwner, localityStateObserver)
            viewModel.locality.observe(viewLifecycleOwner, localityObserver)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_locality, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configure()
    }

    private fun configure() {
        activity?.let {
            viewModel = ViewModelProviders.of(it).get(NewObservationViewModel::class.java)
        }

        mapFragment = childFragmentManager.findFragmentById(R.id.localityFragment_mapView) as MapFragment
        mapFragment.setListener(mapFragmentListener)
    }
}