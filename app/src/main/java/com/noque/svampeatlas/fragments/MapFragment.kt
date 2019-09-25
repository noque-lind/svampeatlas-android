package com.noque.svampeatlas.fragments

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.noque.svampeatlas.extensions.toRectanglePolygon
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Locality
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.fragment_map.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.material.tabs.TabLayout
import com.noque.svampeatlas.extensions.changeColor
import com.noque.svampeatlas.extensions.dpToPx
import com.noque.svampeatlas.utilities.DispatchGroup
import com.noque.svampeatlas.utilities.OpenStreetMapTileProvider
import com.noque.svampeatlas.views.BackgroundView


class MapFragment: Fragment(), ViewTreeObserver.OnGlobalLayoutListener {

    companion object {
        val TAG = "MapFragment"

        val LOCALITY_TAG = "locality"
        val LOCATION_TAG = "location"
    }

    enum class Category {
        REGULAR,
        SATELLITE,
        TOPOGRAPHY
    }

    // Objects
    private var dispatchGroup = DispatchGroup()
    private var markers = mutableListOf<Marker>()
    private var localities = mutableMapOf<String, Locality>()
    private var locationMarker: Marker? = null
    private var selectedMarker: Marker? = null
    private var tileOverlay: TileOverlay? = null

    var localitySelected: ((locality: Locality) -> Unit)? = null


    // Views
    private lateinit var styleSelector: TabLayout
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var backgroundView: BackgroundView
    private lateinit var googleMap: GoogleMap

    // Listeners
    private val onMarkerClickListener = GoogleMap.OnMarkerClickListener {
        when (it.tag) {
            LOCALITY_TAG -> { localities.get(it.id)?.let { localitySelected?.invoke(it) } }
            LOCATION_TAG -> {if (locationMarker?.title != null) it.showInfoWindow() }
        }

        true
    }

    private val onTabChangeListener = object: TabLayout.OnTabSelectedListener {
        override fun onTabReselected(tab: TabLayout.Tab?) {}

        override fun onTabUnselected(tab: TabLayout.Tab?) {}

        override fun onTabSelected(tab: TabLayout.Tab) {
            when (tab.position) {
                0 -> { setType(Category.REGULAR) }
                1 -> { setType(Category.SATELLITE) }
                2 -> { setType(Category.TOPOGRAPHY) }
            }
        }
    }

    // Forced to put the whole fragment as listener, as it was impossible to remove just a listener var/object within the callback
    override fun onGlobalLayout() {
            if (mapFragment.view?.height != 0 && mapFragment.view?.width != 0) {
                mapFragment.view?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                Log.d(TAG, "Leaving dispatch group from onGlobalLayout")
                dispatchGroup.leave()
            }
    }

    private val onMapReadyCallBack = OnMapReadyCallback {
        @SuppressLint("MissingPermission")
                googleMap = it
                googleMap.setOnMarkerClickListener(onMarkerClickListener)
                // Configure map here
                backgroundView.reset()
        Log.d(TAG, "Leaving dispatch group from onMapReadyCallback")
                dispatchGroup.leave()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
    }

    private fun initViews() {
        styleSelector = mapFragment_tabLayout
        backgroundView = fragmentMap_backgroundView
        mapFragment = childFragmentManager.findFragmentById(R.id.fragmentMap_supportMapFragment) as SupportMapFragment
    }

    private fun setupViews() {
        setLoading()

        // Making sure that all the called functions will not be called unless map has been returned.
        dispatchGroup.enter()
        mapFragment.getMapAsync(onMapReadyCallBack)
        mapFragment.view?.viewTreeObserver?.let {
            dispatchGroup.enter()
            it.addOnGlobalLayoutListener(this)
        }
    }

    fun showStyleSelector(show: Boolean) {
        styleSelector.visibility = if (show) View.VISIBLE else View.GONE
        styleSelector.addOnTabSelectedListener(onTabChangeListener)
    }

    fun setType(category: Category) {
        dispatchGroup.notify {
            tileOverlay?.remove()
            tileOverlay = null

            when (category) {
                Category.REGULAR -> googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                Category.SATELLITE -> googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                Category.TOPOGRAPHY -> {
                    googleMap.mapType = GoogleMap.MAP_TYPE_NONE
                    tileOverlay = googleMap.addTileOverlay(TileOverlayOptions().tileProvider(OpenStreetMapTileProvider()))
                }
            }
        }
    }

    fun setOnClickListener(listener: View.OnClickListener?) {
        dispatchGroup.notify {
            googleMap.setOnMapClickListener {
                listener?.onClick(null)
            }

            googleMap.uiSettings?.isRotateGesturesEnabled = false
            googleMap.uiSettings?.isScrollGesturesEnabled = false
            googleMap.uiSettings?.isZoomGesturesEnabled = false
        }
    }

    fun setLoading() {
        // Putting this in notify, as the fragment itself shows a spinner before Google map is ready.
        // By putting it in notify, this function called by the developer is not stopped in OnMapReadyCallback.

        dispatchGroup.notify {
            reset()
            backgroundView.setLoading()
        }
    }

    fun setRegion(coordinate: LatLng) {
        dispatchGroup.notify {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinate, 6F))
        }
    }

    fun setError(error: AppError, handlerTitle: String?, handler: (() -> Unit)?) {
        dispatchGroup.notify {
            backgroundView.reset()
            mapFragment.view?.visibility = View.GONE

            if (handlerTitle != null && handler != null) {
                backgroundView.setErrorWithHandler(error, handlerTitle, handler)
            } else {
                backgroundView.setError(error)
            }
        }
    }

    fun removeAllMarkers() {
        val markersToRemove = markers
        markers.clear()

        markersToRemove.forEach {
            it.remove()
        }
    }

    fun addLocalities(localities: List<Locality>) {
            dispatchGroup.notify {
               reset()

                if (localities.count() > 0) {
                localities.forEach { locality ->
                    val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin)

                    val markerOptions = MarkerOptions()
                        .title(locality.name)
                        .position(locality.location)
                        .icon(BitmapDescriptorFactory.fromBitmap(bitmap.changeColor(ResourcesCompat.getColor(resources, R.color.colorSecondary, null))))

                    googleMap.addMarker(markerOptions)?.let {
                        it.tag = LOCALITY_TAG
                        markers.add(it)
                        this.localities.put(it.id, locality)
                    }
                }
            }
        }
    }

    fun addLocationMarker(location: LatLng, title: String? = null) {
        dispatchGroup.notify {
            reset()

            locationMarker?.remove()
            locationMarker = null

            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_location)

            val markerOptions = MarkerOptions()
                .position(location)
                .title(title)
                .draggable(true)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap.changeColor(ResourcesCompat.getColor(resources, R.color.colorPrimary, null))))

            googleMap.addMarker(markerOptions)?.let {
                it.tag = LOCATION_TAG
                markers.add(it)
                locationMarker = it
            }
        }
    }


    fun addHeatMap(coordinates: List<LatLng>) {
        dispatchGroup.notify {
            reset()

            if (coordinates.count() == 0) {
                return@notify
            }

            val provider = HeatmapTileProvider.Builder()
                .data(coordinates)
                .radius(25)
                .opacity(0.8)

            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireActivity(), R.raw.raw_googlemap_heatmap_style))
            googleMap.addTileOverlay(TileOverlayOptions().tileProvider(provider.build()))
        }
    }



    fun zoomToShowMarkers() {
            dispatchGroup.notify {
                Log.d(TAG, "Zoom to show markers called from notify")

                if (markers.count() > 0) {

                view?.let { view ->
                    val builder = LatLngBounds.Builder()

                    markers.forEach { marker ->
                        builder.include(marker.position)
                    }

                    googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), view.width, view.height, 80.dpToPx(context)))
                }
            }
        }
    }

    fun selectAnnotationAtLocation(latLng: LatLng) {
        dispatchGroup.notify {

            when (selectedMarker?.tag) {
                LOCALITY_TAG -> {
                    val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin)
                    selectedMarker?.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap.changeColor(ResourcesCompat.getColor(resources, R.color.colorSecondary, null))))
                    selectedMarker = null
                }
            }

            markers.find { it.position == latLng }?.let {
                when (it.tag) {
                    LOCALITY_TAG -> {
                        selectedMarker = it
                        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin)
                        it.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap.changeColor(ResourcesCompat.getColor(resources, R.color.colorGreen, null))))
                    }
                }
            }
        }
    }

    fun setPadding(leftPx: Int, topPx: Int, rightPx: Int, bottomPx: Int) {
        dispatchGroup.notify {
            googleMap.setPadding(leftPx, topPx, rightPx, bottomPx)
        }
    }


    fun setRegion(coordinate: LatLng, radius: Double) {
        dispatchGroup.notify {
            val builder = LatLngBounds.Builder()
            coordinate.toRectanglePolygon(radius).forEach {
                builder.include(it)
            }

            val bounds = builder.build()
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
        }
    }

    private fun reset() {
        backgroundView.reset()
        mapFragment.view?.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        Log.d(TAG, "On destroy view")

        markers.clear()
        dispatchGroup.clear()
        mapFragment.view?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
        super.onDestroyView()
    }


    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "On detach")
    }
}