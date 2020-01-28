package com.noque.svampeatlas.fragments.add_observation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.adapters.add_observation.LocalityAdapter
import com.noque.svampeatlas.BuildConfig
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.R
import com.noque.svampeatlas.services.LocationService
import com.noque.svampeatlas.fragments.MapFragment
import com.noque.svampeatlas.fragments.NearbyFragment
import com.noque.svampeatlas.models.Locality
import com.noque.svampeatlas.models.Observation
import com.noque.svampeatlas.view_models.NewObservationViewModel
import kotlinx.android.synthetic.main.fragment_add_observation_locality.*

class LocalityFragment: Fragment() {

    companion object {
        val TAG = "LocalityFragment"
    }

    // Views
    private var mapFragment: MapFragment? = null
    private var recyclerView: RecyclerView? = null
    private var retryButton: ImageButton? = null
    private var markerImageView: ImageView? = null

    // View models
    private val newObservationViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(NewObservationViewModel::class.java)
    }

    // Adapters
    private val localityAdapter by lazy {
        val adapter = LocalityAdapter()

        adapter.localitySelected = {
            newObservationViewModel.setLocality(it)
        }

        adapter
    }

    // Listeners

    private val retryButtonClicked = View.OnClickListener {
        newObservationViewModel.resetLocationData()
    }


    private val mapFragmentListener by lazy {
        object: MapFragment.Listener {
            override fun onClick() {}
            override fun observationSelected(observation: Observation) {}
            override fun localitySelected(locality: Locality) { newObservationViewModel.setLocality(locality) }
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
                        markerImageView?.translationX = x - originX
                        markerImageView?.translationY = y - originY
                    }

                    MotionEvent.ACTION_UP -> {
                        val location: IntArray = IntArray(2)
                        view.getLocationInWindow(location)
                        val finalX = location.first().toFloat()
                        val finalY = location.last().toFloat()

                        mapFragment?.getCoordinatesFor(finalX + (view.width / 2), finalY + view.height)?.let { newObservationViewModel.setCoordinate(it, 5F) }

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
        return inflater.inflate(R.layout.fragment_add_observation_locality, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
        setupViewModels()
    }

    private fun initViews() {
        recyclerView = localityFragment_recyclerView
        mapFragment = childFragmentManager.findFragmentById(R.id.localityFragment_mapView) as MapFragment
        retryButton = localityFragment_retryButton
        markerImageView = localityFragment_markerImageView
    }

    private fun setupViews() {
        recyclerView?.apply {

            this.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mapFragment?.setPadding(0, 0, 0, this@apply.height + this@apply.marginBottom)
                    mapFragment?.setRegionToShowMarkers()
                    this@apply.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })

            adapter = localityAdapter
            val linearLayoutManager = LinearLayoutManager(context)
            linearLayoutManager.orientation = RecyclerView.HORIZONTAL
            layoutManager = linearLayoutManager
        }

        mapFragment?.setListener(mapFragmentListener, false)
        mapFragment?.setType(MapFragment.Category.TOPOGRAPHY)
        retryButton?.setOnClickListener(retryButtonClicked)
        markerImageView?.setOnTouchListener(markerOnTouchListener)

    }

    private fun setupViewModels() {
            newObservationViewModel.localityState.observe(viewLifecycleOwner, Observer {
                when (it) {
                    is State.Items -> {
                        localityAdapter.configure(it.items)
                        mapFragment?.addLocalities(it.items)
                        mapFragment?.setRegionToShowMarkers()
                    }

                    is State.Loading -> {
                        mapFragment?.setLoading()
                    }

                    is State.Error -> {
                        when (it.error) {
                            is LocationService.Error.PermissionDenied -> {
                                mapFragment?.setError(
                                    it.error,
                                    resources.getString(R.string.locationservice_error_permissions_handler)
                                ) {
                                    val intent = Intent()
                                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    val uri =
                                        Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                    intent.data = uri
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    startActivity(intent)
                                }
                            }
                            else -> { mapFragment?.setError(it.error, null, null) }
                        }
                    }

                    is State.Empty -> {
                        mapFragment?.removeAllMarkers()
                    }
                }
            })

            newObservationViewModel.locality.observe(viewLifecycleOwner, Observer {
                it?.let {
                    recyclerView?.scrollToPosition(localityAdapter.setSelected(it))
                    mapFragment?.setSelectedLocalityAnnotation(it.location)
                }
            })

            newObservationViewModel.coordinate.observe(viewLifecycleOwner, Observer {
                it?.let {
                    mapFragment?.addLocationMarker(it, resources.getString(R.string.localityFragment_sightingLocation))
                }
            })
    }

    override fun onDestroyView() {
        Log.d(TAG, "On destroy view")

        recyclerView?.adapter = null

        mapFragment = null
        recyclerView = null
        retryButton = null
        markerImageView = null

        super.onDestroyView()
    }
}