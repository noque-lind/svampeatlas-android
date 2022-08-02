package com.noque.svampeatlas.fragments.add_observation

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.marginBottom
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.adapters.add_observation.LocalityAdapter
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.openSettings
import com.noque.svampeatlas.fragments.MapFragment
import com.noque.svampeatlas.fragments.TermsFragment
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.utilities.safeAutoCleared
import com.noque.svampeatlas.view_models.NewObservationViewModel
import kotlinx.android.synthetic.main.fragment_add_observation_locality.*
import java.util.*

class LocalityFragment: Fragment() {

    companion object {
        const val TAG = "LocalityFragment"
    }

    private var onGlobalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    // Views
    private var mapFragment by safeAutoCleared<MapFragment> {
        it?.setListener(null)
    }
    private var recyclerView  by autoCleared<RecyclerView>() {
        it?.viewTreeObserver?.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        onGlobalLayoutListener = null
        it?.adapter = null
    }
    private var retryButton by autoCleared<ImageButton>()
    private var markerImageView by autoCleared<ImageView>()
    private var precisionLabel by autoCleared<TextView>()

    // View models
    private val newObservationViewModel: NewObservationViewModel by viewModels({ requireParentFragment() })

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

            var hasStarted = false
            var originX: Float = 0F
            var originY: Float = 0F

            override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
                val x = motionEvent.rawX
                val y = motionEvent.rawY

                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        val location = IntArray(2)
                        view.getLocationInWindow(location)
                        originX = location.first().toFloat()
                        originY = location.last().toFloat()
                    }

                    MotionEvent.ACTION_MOVE -> {
                        if ((x - originX > 500 || y - originY > 500) || hasStarted) {
                            hasStarted = true
                            markerImageView.translationX = x - (originX + ((markerImageView.width.toFloat())))
                            markerImageView.translationY = y - (originY + ((markerImageView.height.toFloat()) * 2))
                        } else {
                            markerImageView.translationX = x / 4 - originX
                            markerImageView.translationY = (y - (originY + ((markerImageView.height.toFloat())))) * 0.2.toFloat()
                        }
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        if (hasStarted) {
                            val location: IntArray = IntArray(2)
                            view.getLocationInWindow(location)
                            val finalX = location.first().toFloat()
                            val finalY = location.last().toFloat()

                            mapFragment?.getCoordinatesFor(finalX + (view.width / 2), finalY + view.height)?.let { newObservationViewModel.setCoordinateState(
                                State.Items(Location(Date(), it, 5F))
                            ) }
                        } else {
                            val bundle = Bundle()
                            bundle.putSerializable(TermsFragment.KEY_TYPE, TermsFragment.Type.LOCALITYHELPER)
                            val dialog = TermsFragment()
                            dialog.arguments = bundle
                            dialog.show(childFragmentManager, null)
                        }

                        markerImageView.translationX = 0F
                        markerImageView.translationY = 0F
                        hasStarted = false
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
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
        precisionLabel = localityFragment_precisionLabel
    }

    private fun setupViews() {
        recyclerView.apply {
            onGlobalLayoutListener = object: ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    mapFragment?.setPadding(0, 0, 0, this@apply.height + this@apply.marginBottom)
                    mapFragment?.setRegionToShowMarkers()
                    this@apply.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
            viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
            adapter = localityAdapter
            layoutManager = LinearLayoutManager(context).apply { orientation = RecyclerView.HORIZONTAL }
        }

        mapFragment?.setListener(mapFragmentListener)
        mapFragment?.setType(MapFragment.Category.REGULAR)
        retryButton.setOnClickListener(retryButtonClicked)
        markerImageView.setOnTouchListener(markerOnTouchListener)
    }

    private fun setupViewModels() {
        newObservationViewModel.localitiesState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Items -> {
                    localityAdapter.configure(it.items)
                    mapFragment?.addLocalities(it.items)
                }

                is State.Loading -> mapFragment?.setLoading()

                is State.Error -> {
                    mapFragment?.stopLoading()
                }

                is State.Empty -> {}
            }
        })

            newObservationViewModel.locality.observe(viewLifecycleOwner, Observer {
                it?.let {
                    recyclerView.scrollToPosition(localityAdapter.setSelected(it))
                    mapFragment?.setSelectedLocalityAnnotation(it.location)
                }
            })

            newObservationViewModel.coordinateState.observe(viewLifecycleOwner, Observer {
                when (it) {
                    is State.Items -> {
                        mapFragment?.addLocationMarker(it.items.latLng, resources.getString(R.string.locationAnnotation_title), it.items.accuracy.toDouble())
                        mapFragment?.setRegion(it.items.latLng)
                        precisionLabel.text = resources.getString(R.string.precisionLabel, it.items.accuracy)
                    }

                    is State.Loading -> {
                        mapFragment?.setLoading()
                        precisionLabel.text = "Finder placering"
                    }

                    is State.Empty -> {
                        mapFragment?.removeAllMarkers()
                    }
                    is State.Error -> {
                        mapFragment?.setError(it.error) {
                            when (it) {
                                RecoveryAction.OPENSETTINGS -> openSettings()
                                RecoveryAction.TRYAGAIN -> newObservationViewModel.resetLocationData()
                                else -> {}
                            }
                        }
                    }
                }
            })
    }
}