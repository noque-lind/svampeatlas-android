package com.noque.svampeatlas.fragments

import android.location.Location
import android.os.Bundle
import android.transition.TransitionManager
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.model.LatLng

import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.openSettings
import com.noque.svampeatlas.models.Locality
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.models.RecoveryAction
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.LocationService
import com.noque.svampeatlas.view_models.NearbyObservationsViewModel
import com.noque.svampeatlas.views.BlankActivity
import com.noque.svampeatlas.views.ObservationView
import kotlinx.android.synthetic.main.fragment_nearby.*


class NearbyFragment : Fragment(), MapSettingsFragment.Listener {
    override fun newSearch() {
        mapFragment?.setLoading()
        locationService.start()
    }

    override fun radiusChanged(value: Int) {
        settings.radius = value
        setDistanceLabel()
    }

    override fun ageChanged(value: Int) {
        settings.ageInYears = value
        setAgeLabel()
    }

    override fun clearAllSet(value: Boolean) {
        settings.clearAll = value
    }


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

    data class Settings(var radius: Int, var ageInYears: Int, var clearAll: Boolean)

    // Objects

    private val rootConstraintSet by lazy {
        val constraintSet = ConstraintSet()
        constraintSet.clone(requireContext(), R.layout.fragment_nearby)
        constraintSet
    }
    private val observationConstraintSet by lazy {
        val constraintSet = ConstraintSet()
        constraintSet.clone(requireContext(), R.layout.fragment_nearby_observation)
        constraintSet
    }

    private var settings = Settings(1000, 1, false)


    // Views

    private var toolbar: Toolbar? = null
    private var mapFragment: MapFragment? = null
    private var observationView: ObservationView? = null
    private var settingsButton: ImageButton? = null
    private var markerImageView: ImageView? = null
    private var distanceLabel: TextView? = null
    private var ageLabel: TextView? = null


    // View models

    private val nearbyObservationsViewModel by lazy {
        ViewModelProviders.of(this).get(NearbyObservationsViewModel::class.java)
    }

    private val locationService by lazy {
        val locationService = LocationService(requireContext())
        locationService.setListener(object: LocationService.Listener {
            override fun locationRetrieved(location: Location) {
                mapFragment?.setShowMyLocation(true)
                nearbyObservationsViewModel.getObservationsNearby(LatLng(location.latitude, location.longitude), settings)
            }

            override fun locationRetrievalError(error: LocationService.Error) {
                mapFragment?.setError(error) {
                    if (error.recoveryAction == RecoveryAction.OPENSETTINGS) {
                        openSettings()
                    }
                }
            }

            override fun requestPermission(permissions: Array<out String>, requestCode: Int) {
                requestPermissions(permissions, requestCode)
            }
        })

        locationService
    }

    private val mapFragmentListener by lazy {
        object: MapFragment.Listener {
            override fun onClick() { hideObservationView() }
            override fun observationSelected(observation: Observation) { showObservationView(observation) }
            override fun localitySelected(locality: Locality) {}
        }
    }

    private val observationViewOnClick by lazy {
        View.OnClickListener {
            observationView?.observation?.let {
                val action = NearbyFragmentDirections.actionGlobalMushroomDetailsFragment(
                    it.id,
                    DetailsFragment.TakesSelection.NO,
                    DetailsFragment.Type.OBSERVATIONWITHSPECIES,
                    null,
                    null
                )

                findNavController().navigate(action)
            }
        }
    }

    private val settingsButtonOnClick  by lazy {
        View.OnClickListener {
            val dialog = MapSettingsFragment()
            val bundle = Bundle()
            bundle.putInt(MapSettingsFragment.KEY_RADIUS, settings.radius)
            bundle.putInt(MapSettingsFragment.KEY_AGE, settings.ageInYears)

            dialog.setTargetFragment(this, 0)
            dialog.arguments = bundle
            dialog.show(requireFragmentManager(), null)
        }
    }


    private val markerOnTouchListener by lazy {
        object: View.OnTouchListener {

            var originX: Float = 0F
            var originY: Float = 0F

            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                val x = motionEvent.rawX
                val y = motionEvent.rawY
                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val location: IntArray = IntArray(2)
                        view.getLocationInWindow(location)
                        originX = location.first().toFloat()
                        originY = location.last().toFloat() + 250

                    }

                    MotionEvent.ACTION_MOVE -> {
                        val deltaX =  x -originX
                        val deltaY = y - originY

                        markerImageView?.translationX = deltaX
                        markerImageView?.translationY = deltaY
                    }

                    MotionEvent.ACTION_UP -> {

                        mapFragment?.getCoordinatesFor(x, y)?.let {
                           nearbyObservationsViewModel.getObservationsNearby(it, settings)
                       }

                        markerImageView?.translationX = 0F
                        markerImageView?.translationY = 0F
                    }
                }

                return true
            }

        }
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
    }

    private fun initViews() {
        toolbar = nearbyFragment_toolbar
        mapFragment = childFragmentManager.findFragmentById(R.id.nearbyFragment_mapFragment) as MapFragment
        observationView = nearbyFragment_observationView
        settingsButton = nearbyFragment_settingsButton
        markerImageView = nearbyFragment_markerImageView
        distanceLabel = nearbyFragment_distanceLabel
        ageLabel = nearbyFragment_ageLabel

    }

    private fun setupViews() {
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
        mapFragment?.showStyleSelector(true)
        mapFragment?.setListener(mapFragmentListener)
        observationView?.apply {
            setOnClickListener(observationViewOnClick)
        }

        settingsButton?.setOnClickListener(settingsButtonOnClick)
        markerImageView?.setOnTouchListener(markerOnTouchListener)

        setDistanceLabel()
        setAgeLabel()
    }

    private fun setupViewModels() {
        nearbyObservationsViewModel.observationsState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Loading -> { mapFragment?.setLoading() }
                is State.Error -> { mapFragment?.setError(it.error, null) }
                is State.Items -> {
                    mapFragment?.clearCircleOverlays()
                    mapFragment?.addObservationMarkers(it.items.first)
                    mapFragment?.setRegion(it.items.second.last().coordinate, (it.items.second.last().radius * 1.1).toInt())
                    it.items.second.forEach {  mapFragment?.addCircleOverlay(it.coordinate, it.radius) }
                }
                is State.Empty -> { locationService.start() }
            }
        })
    }


    private fun showObservationView(observation: Observation) {
        observationView?.configure(observation, true)
        TransitionManager.beginDelayedTransition(nearbyFragment_root)
        observationConstraintSet.applyTo(nearbyFragment_root)
    }

   private fun hideObservationView() {
        TransitionManager.beginDelayedTransition(nearbyFragment_root)
        rootConstraintSet.applyTo(nearbyFragment_root)
    }

    private fun setDistanceLabel() {
        distanceLabel?.text = "${String.format("%.1f", settings.radius.toDouble() / 1000)} km."
    }


    private fun setAgeLabel() {
        ageLabel?.text = resources.getString(R.string.mapViewSettingsView_year, settings.ageInYears)
    }

    override fun onDestroyView() {
        toolbar = null
        mapFragment?.setListener(null)
        mapFragment = null
        observationView = null
        settingsButton = null
        markerImageView = null
        distanceLabel = null
        ageLabel = null
        super.onDestroyView()
    }
}
