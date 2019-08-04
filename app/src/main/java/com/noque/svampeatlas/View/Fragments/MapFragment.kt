package com.noque.svampeatlas.View.Fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.internal.maps.zzt
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks.await
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.noque.svampeatlas.Extensions.toRectanglePolygon
import com.noque.svampeatlas.Model.AppError
import com.noque.svampeatlas.Model.Locality
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.fragment_map.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker


interface MapFragmentDelegate {
    fun mapReady()
    fun localityPicked(locality: Locality) {}
}

class MapFragment: Fragment() {

    lateinit var mapFragment: SupportMapFragment
    private var googleMap: GoogleMap? = null
    private var listener: MapFragmentDelegate? = null

    private var localities = mutableMapOf<String, Locality>()

    private var markers = mutableListOf<Marker>()

    private val onMarkerClickListener = object: GoogleMap.OnMarkerClickListener {
        override fun onMarkerClick(marker: Marker): Boolean {

            when (marker.tag) {
                "locality" -> {
                    localities[marker.id]?.let {
                        listener?.localityPicked(it)
                    }
                }
            }


            return true
        }

    }


    private val onMapReadyCallback = object: OnMapReadyCallback {
        @SuppressLint("MissingPermission")
        override fun onMapReady(googleMap: GoogleMap?) {
            googleMap?.isMyLocationEnabled = true
            this@MapFragment.googleMap = googleMap
            listener?.mapReady()

            googleMap?.setOnMarkerClickListener(onMarkerClickListener)
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
        mapFragment = childFragmentManager.findFragmentById(R.id.fragmentMap_mapView) as SupportMapFragment
    }

    fun setListener(listener: MapFragmentDelegate) {
        mapFragment.getMapAsync(onMapReadyCallback)
        this.listener = listener
    }

    fun setLoading() {
        fragmentMap_backgroundView.setLoading()
    }

    fun setError(error: AppError) {
        fragmentMap_backgroundView.setError(error)
    }


    fun addHeatMap(coordinates: List<LatLng>) {
        if (coordinates.count() == 0) {
            return
        }

        mapFragment.getMapAsync {
            val provider = HeatmapTileProvider.Builder()
                .data(coordinates)
                .radius(20)
                .opacity(0.8)
            val test = it.addTileOverlay(TileOverlayOptions().tileProvider(provider.build()))
        }
    }

    fun addLocalities(localities: List<Locality>) {
        mapFragment.getMapAsync { googleMap ->
            localities.forEach {
                val markerOptions = MarkerOptions().title(it.name).position(it.location)
                val marker = googleMap.addMarker(markerOptions)
                marker.tag = "locality"
                markers.add(marker)
                this.localities.put(marker.id, it)
            }
        }
    }

    fun zoomToShowMarkers() {

        mapFragment.getMapAsync { googleMap ->



            val builder = LatLngBounds.Builder()
            markers.forEach {
                builder.include(it.position)
            }

            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), view!!.width, view!!.height, 10))
        }
    }




    fun setRegion(coordinate: LatLng, radius: Double) {
        val builder = LatLngBounds.Builder()

        coordinate.toRectanglePolygon(radius).forEach {
            builder.include(it)
        }

        val bounds = builder.build()

        googleMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
    }

}

/**
 * Manipulates the map once available.
 * This callback is triggered when the map is ready to be used.
 * This is where we can add markers or lines, add listeners or move the camera. In this case,
 * we just add a marker near Sydney, Australia.
 * If Google Play services is not installed on the device, the user will be prompted to install
 * it inside the SupportMapFragment. This method will only be triggered once the user has
 * installed Google Play services and returned to the app.
// */
//override fun onMapReady(googleMap: GoogleMap) {
//
//    mMap = googleMap
//
//    // Add a marker in Sydney and move the camera
//    val sydney = LatLng(-34.0, 151.0)
//    mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
//    mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
//}