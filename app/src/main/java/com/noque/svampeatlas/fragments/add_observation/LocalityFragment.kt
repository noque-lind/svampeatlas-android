package com.noque.svampeatlas.fragments.add_observation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageButton
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
    }

    private fun setupViews() {
        recyclerView?.apply {

            this.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mapFragment?.setPadding(0, 0, 0, this@apply.height + this@apply.marginBottom)
                    mapFragment?.zoomToShowMarkers()
                    this@apply.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })

            adapter = localityAdapter
            val linearLayoutManager = LinearLayoutManager(context)
            linearLayoutManager.orientation = RecyclerView.HORIZONTAL
            layoutManager = linearLayoutManager
        }

        mapFragment?.localitySelected = {
            newObservationViewModel.setLocality(it)
        }

        mapFragment?.setType(MapFragment.Category.TOPOGRAPHY)
        retryButton?.setOnClickListener(retryButtonClicked)

    }

    private fun setupViewModels() {
            newObservationViewModel.localityState.observe(viewLifecycleOwner, Observer {
                when (it) {
                    is State.Items -> {
                        localityAdapter.configure(it.items)
                        mapFragment?.addLocalities(it.items)
                        mapFragment?.zoomToShowMarkers()
                    }

                    is State.Loading -> {
                        mapFragment?.setLoading()
                    }

                    is State.Error -> {
                        Log.d(TAG, it.error.toString())

                        (it.error as? LocationService.Error)?.let {
                            when (it) {
                                is LocationService.Error.PermissionDenied -> {
                                    mapFragment?.setError(it, resources.getString(R.string.locationservice_error_permissions_handler)) {
                                        val intent = Intent()
                                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                        intent.data = uri
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        startActivity(intent)
                                    }
                                }
                            }
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
                    mapFragment?.selectAnnotationAtLocation(it.location)
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

        super.onDestroyView()
    }
}