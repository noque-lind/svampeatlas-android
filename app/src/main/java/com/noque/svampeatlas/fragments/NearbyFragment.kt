package com.noque.svampeatlas.fragments

import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.model.LatLng

import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.LocationService
import com.noque.svampeatlas.view_models.NearbyObservationsViewModel
import com.noque.svampeatlas.views.BlankActivity
import kotlinx.android.synthetic.main.fragment_nearby.*

class NearbyFragment : Fragment() {

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        locationService.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        val TAG = "NearbyFragment"
    }

    // Objects

    private val locationService by lazy {
        val locationService = LocationService(requireContext())
        locationService.setListener(object: LocationService.Listener {
            override fun locationRetrieved(location: Location) {
                nearbyObservationsViewModel.getObservationsNearby(LatLng(location.latitude, location.longitude))
            }

            override fun locationRetrievalError(error: LocationService.Error) {
                when (error) {
                    is LocationService.Error.PermissionDenied -> {
                        mapFragment?.setError(error, getString(R.string.locationservice_error_permissions_handler)) {

                        }
                    }
                }
            }

            override fun requestPermission(permissions: Array<out String>, requestCode: Int) {
                requestPermissions(permissions, requestCode)
            }
        })

        locationService
    }


    // Views

    private var toolbar: Toolbar? = null
    private var mapFragment: MapFragment? = null


    // View models

    private val nearbyObservationsViewModel by lazy {
        ViewModelProviders.of(this).get(NearbyObservationsViewModel::class.java)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nearby, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
        setupViewModels()

        locationService.start()
    }

    private fun initViews() {
        toolbar = nearbyFragment_toolbar
        mapFragment = childFragmentManager.findFragmentById(R.id.nearbyFragment_mapFragment) as MapFragment
    }

    private fun setupViews() {
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
        mapFragment?.showStyleSelector(true)
    }

    private fun setupViewModels() {
        nearbyObservationsViewModel.observationsState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Loading -> {
                    mapFragment?.setLoading()
                }

                is State.Error -> {
                    mapFragment?.setError(it.error, null, null)
                }

                is State.Items -> {
                    Log.d(TAG, it.items.toString())
                }
            }
        })
    }


}
