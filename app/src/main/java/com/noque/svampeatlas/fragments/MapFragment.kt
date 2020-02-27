package com.noque.svampeatlas.fragments

import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.noque.svampeatlas.extensions.toRectanglePolygon
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.fragment_map.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.material.tabs.TabLayout
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.noque.svampeatlas.extensions.changeColor
import com.noque.svampeatlas.extensions.dpToPx
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.utilities.DispatchGroup
import com.noque.svampeatlas.utilities.OpenStreetMapTileProvider
import com.noque.svampeatlas.views.BackgroundView


data class ObservationItem(val observation: Observation) : ClusterItem {
    override fun getSnippet(): String? {
        return null
    }

    override fun getTitle(): String? {
        return null
    }

    override fun getPosition(): LatLng {
        return observation.coordinate
    }
}

class MapFragment : Fragment(), ViewTreeObserver.OnGlobalLayoutListener {

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


    interface Listener {
        fun onClick()
        fun observationSelected(observation: Observation)
        fun localitySelected(locality: Locality)
    }

    // Objects

    private var dispatchGroup = DispatchGroup()
    private var listener: Listener? = null
    private var clusterManager: ClusterManager<ObservationItem>? = null

    private var markers = mutableListOf<Marker>()
    private var localities = mutableMapOf<String, Locality>()

    private var locationMarker: Marker? = null
    private var selectedMarker: Marker? = null
    private var tileOverlay: TileOverlay? = null
    private var circleOverlays = mutableListOf<Circle>()

    // Views
    private var styleSelector: TabLayout? = null
    private var mapFragment: SupportMapFragment? = null
    private var backgroundView: BackgroundView? = null
    private var googleMap: GoogleMap? = null

    // Listeners
    private val onClickListener by lazy {
        GoogleMap.OnMapClickListener {
            listener?.onClick()
        }
    }


    private val onMarkerClickListener by lazy {
        GoogleMap.OnMarkerClickListener {
            when (it.tag) {
                LOCALITY_TAG -> {
                    localities[it.id]?.let { listener?.localitySelected(it) }
                }
                LOCATION_TAG -> {
                    if (locationMarker?.title != null) it.showInfoWindow()
                }
            }

            true
        }
    }

    private val onTabChangeListener by lazy {
        object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        setType(Category.REGULAR)
                    }
                    1 -> {
                        setType(Category.SATELLITE)
                    }
                    2 -> {
                        setType(Category.TOPOGRAPHY)
                    }
                }
            }
        }
    }

    private val onClusterClickListener by lazy {ClusterManager.OnClusterClickListener<ObservationItem> {
        val builder = LatLngBounds.builder()
        it.items.forEach {
            builder.include(it.position)
        }
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 300))
        true
    }}

    private val onClusterItemListener by lazy {
        ClusterManager.OnClusterItemClickListener<ObservationItem> {
            listener?.observationSelected(it.observation)
            false
        }
    }


    // Forced to put the whole fragment as listener, as it was impossible to remove just a listener var/object within the callback
    override fun onGlobalLayout() {
        if (mapFragment?.view?.height != 0 && mapFragment?.view?.width != 0) {
            mapFragment?.view?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            dispatchGroup.leave()
        }
    }

    private val onMapReadyCallBack by lazy {
        OnMapReadyCallback {
            it.setOnMapClickListener(onClickListener)
            googleMap = it
            googleMap?.uiSettings?.isIndoorLevelPickerEnabled = false
            googleMap?.uiSettings?.isMapToolbarEnabled = false
            googleMap?.uiSettings?.isCompassEnabled = false
            googleMap?.setOnMarkerClickListener(onMarkerClickListener)
            setType(Category.REGULAR)
            dispatchGroup.leave()
        }
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
        mapFragment =
            childFragmentManager.findFragmentById(R.id.fragmentMap_supportMapFragment) as SupportMapFragment
    }

    private fun setupViews() {
        backgroundView?.setLoading()

        // Making sure that all the called functions will not be called unless map has been returned.
        dispatchGroup.enter()
        mapFragment?.getMapAsync(onMapReadyCallBack)
        mapFragment?.view?.viewTreeObserver?.let {
            dispatchGroup.enter()
            it.addOnGlobalLayoutListener(this)
        }
    }

    fun showStyleSelector(show: Boolean) {
        styleSelector?.visibility = if (show) View.VISIBLE else View.GONE
        styleSelector?.addOnTabSelectedListener(onTabChangeListener)
    }

    fun setType(category: Category) {
        dispatchGroup.notify {
            tileOverlay?.remove()
            tileOverlay = null

            when (category) {
                Category.REGULAR -> googleMap?.mapType = GoogleMap.MAP_TYPE_TERRAIN
                Category.SATELLITE -> googleMap?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                Category.TOPOGRAPHY -> {
                    googleMap?.mapType = GoogleMap.MAP_TYPE_NONE
                    tileOverlay = googleMap?.addTileOverlay(
                        TileOverlayOptions().tileProvider(OpenStreetMapTileProvider())
                    )
                }
            }
        }
    }

    fun setListener(listener: Listener?, disableGestures: Boolean) {
        this.listener = listener

        if (disableGestures) {
            dispatchGroup.notify {
                googleMap?.uiSettings?.isRotateGesturesEnabled = false
                googleMap?.uiSettings?.isScrollGesturesEnabled = false
                googleMap?.uiSettings?.isZoomControlsEnabled = false
                googleMap?.setOnMarkerClickListener(null)
                googleMap?.uiSettings?.isMapToolbarEnabled = false
                googleMap?.uiSettings?.setAllGesturesEnabled(false)
            }
        }
    }


    fun setLoading() {
//         Putting this in notify, as the fragment itself shows a spinner before Google map is ready.
//         By putting it in notify, this function called by the developer is not stopped in OnMapReadyCallback.

        dispatchGroup.notify {
            backgroundView?.reset()
            backgroundView?.setLoading()
        }
    }

    fun setError(error: AppError, handler: ((RecoveryAction?) -> Unit)?) {
        dispatchGroup.notify {
            backgroundView?.reset()
            mapFragment?.view?.visibility = View.GONE

            if (error.recoveryAction != null && handler != null) {
                backgroundView?.setErrorWithHandler(error, error.recoveryAction, handler)
            } else {
                backgroundView?.setError(error)
            }
        }
    }

    fun setPadding(leftPx: Int, topPx: Int, rightPx: Int, bottomPx: Int) {
        dispatchGroup.notify {
            googleMap?.setPadding(leftPx, topPx, rightPx, bottomPx)
        }
    }


    fun setRegion(coordinate: LatLng, radius: Int) {
        dispatchGroup.notify {
            val builder = LatLngBounds.Builder()
            coordinate.toRectanglePolygon(radius).forEach {
                builder.include(it)
            }

            val bounds = builder.build()
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
        }
    }

    fun setRegion(coordinate: LatLng) {
        dispatchGroup.notify {
            if ((googleMap?.cameraPosition?.zoom ?: 0f) <= 13) {
               googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(coordinate, 13f)))
            } else {
                googleMap?.moveCamera(CameraUpdateFactory.newLatLng(coordinate))
            }
        }
    }

    fun setRegionToShowMarkers() {
        dispatchGroup.notify {
            if (markers.count() > 0) {
                view?.let { view ->
                    val builder = LatLngBounds.Builder()

                    markers.forEach { marker ->
                        builder.include(marker.position)
                    }

                    googleMap?.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            builder.build(),
                            view.width,
                            view.height,
                            80.dpToPx(context)
                        )
                    )
                }
            }
        }
    }

    fun setSelectedLocalityAnnotation(latLng: LatLng) {
        dispatchGroup.notify {
            when (selectedMarker?.tag) {
                LOCALITY_TAG -> {
                    val bitmap =
                        BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin)
                    selectedMarker?.setIcon(
                        BitmapDescriptorFactory.fromBitmap(
                            bitmap.changeColor(
                                ResourcesCompat.getColor(resources, R.color.colorSecondary, null)
                            )
                        )
                    )
                    selectedMarker = null
                }
            }

            markers.find { it.position == latLng }?.let {
                when (it.tag) {
                    LOCALITY_TAG -> {
                        selectedMarker = it
                        val bitmap =
                            BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin)
                        it.setIcon(
                            BitmapDescriptorFactory.fromBitmap(
                                bitmap.changeColor(
                                    ResourcesCompat.getColor(resources, R.color.colorGreen, null)
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    fun setShowMyLocation(show: Boolean) {
        googleMap?.isMyLocationEnabled = show
    }

    fun addHeatMap(coordinates: List<LatLng>) {
        dispatchGroup.notify {
            reset()

            if (coordinates.isNotEmpty()) {
                val provider = HeatmapTileProvider.Builder()
                    .data(coordinates)
                    .radius(25)
                    .opacity(0.8)

                googleMap?.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                        requireActivity(),
                        R.raw.raw_googlemap_heatmap_style
                    )
                )
                googleMap?.addTileOverlay(TileOverlayOptions().tileProvider(provider.build()))
            }
        }
    }

    fun addLocalities(localities: List<Locality>) {
        dispatchGroup.notify {
            reset()

            localities.forEach { locality ->
                val bitmap =
                    BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin)

                val markerOptions = MarkerOptions()
                    .title(locality.name)
                    .position(locality.location)
                    .icon(
                        BitmapDescriptorFactory.fromBitmap(
                            bitmap.changeColor(
                                ResourcesCompat.getColor(
                                    resources,
                                    R.color.colorSecondary,
                                    null
                                )
                            )
                        )
                    )

                googleMap?.addMarker(markerOptions)?.let {
                    it.tag = LOCALITY_TAG
                    markers.add(it)
                    this.localities.put(it.id, locality)
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
                .icon(
                    BitmapDescriptorFactory.fromBitmap(
                        bitmap.changeColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.colorPrimary,
                                null
                            )
                        )
                    )
                )

            googleMap?.addMarker(markerOptions)?.let {
                it.tag = LOCATION_TAG
                markers.add(it)
                locationMarker = it
            }
        }
    }

    fun addObservationMarkers(observations: List<Observation>) {
        dispatchGroup.notify {
            reset()

            if (clusterManager != null) {
                clusterManager?.clearItems()

            } else {
                clusterManager = ClusterManager(requireContext(), googleMap)
            }

            googleMap?.setOnCameraIdleListener(clusterManager)
            googleMap?.setOnMarkerClickListener(clusterManager)
            clusterManager?.setOnClusterClickListener(onClusterClickListener)
            clusterManager?.setOnClusterItemClickListener(onClusterItemListener)

            observations.forEach { observation ->
                clusterManager?.addItem(ObservationItem(observation))
            }
        }
    }

    fun addCircleOverlay(center: LatLng, radius: Int) {
        dispatchGroup.notify {
            googleMap?.addCircle(
                CircleOptions()
                    .center(center)
                    .strokeWidth(5F)
                    .radius(radius.toDouble())
                    .zIndex(10F)
                    .fillColor(
                        ColorUtils.setAlphaComponent(
                            resources.getColor(R.color.colorPrimary),
                            60
                        )
                    )
            )?.let {
                circleOverlays.add(it)
            }
        }
    }

    fun getCoordinatesFor(x: Float, y: Float): LatLng? {
        val location: IntArray = IntArray(2)
        mapFragment?.view?.getLocationInWindow(location)
        return googleMap?.projection?.fromScreenLocation(Point(x.toInt() - location.first(), y.toInt() - location.last()))
    }

    fun clearCircleOverlays() {
        circleOverlays.forEach {
            it.remove()
        }
    }


    fun removeAllMarkers() {
        markers.forEach { it.remove() }
        markers.clear()
    }


    private fun reset() {
        backgroundView?.reset()
        mapFragment?.view?.visibility = View.VISIBLE
    }


    override fun onDestroyView() {
        Log.d(TAG, "On destroy view")
        clusterManager?.clearItems()
        googleMap?.setOnCameraIdleListener(null)
        googleMap?.setOnMarkerClickListener(null)
        googleMap?.clear()
        clusterManager = null

        listener = null
        locationMarker = null
        selectedMarker = null
        tileOverlay = null

        markers.clear()
        localities.clear()
        dispatchGroup.clear()
        circleOverlays.clear()

        mapFragment?.view?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
        mapFragment = null
        styleSelector = null
        backgroundView = null
        googleMap = null
        super.onDestroyView()
    }
}