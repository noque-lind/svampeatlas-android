package com.noque.svampeatlas.View.Fragments


import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.Extensions.italized
import com.noque.svampeatlas.Extensions.upperCased
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.ViewModel.DetailsViewModel
import kotlinx.android.synthetic.main.fragment_details.*
import com.noque.svampeatlas.R
import com.noque.svampeatlas.Services.LocationService
import com.noque.svampeatlas.Services.LocationServiceDelegate
import com.noque.svampeatlas.Utilities.Geometry
import com.noque.svampeatlas.View.DescriptionView
import com.noque.svampeatlas.Model.State
import java.lang.Exception
import java.lang.ref.WeakReference


class DetailsFragment : Fragment(){
    var locationService: LocationService? = null
    lateinit var detailsViewModel: DetailsViewModel
    lateinit var mapFragment: MapFragment
    lateinit var mushroom: Mushroom

    private var locationServiceListener = object: LocationServiceDelegate {
        override fun locationRetrieved(location: Location) {
            val geometry = Geometry(
                LatLng(location.latitude, location.longitude),
                80000.0,
                Geometry.Type.RECTANGLE
            )

            mapFragment.setRegion(geometry.coordinate, geometry.radius)
        detailsViewModel.getHeatMapObservations(mushroom.id, geometry)
        }

        override fun locationRetrievalError(exception: Exception) {
            Log.d("DetailsFragment", exception.toString())
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupView()
        attachObservers()
    }

    override fun onStop() {
        super.onStop()
//        (activity as AppCompatActivity).let {
//            it.supportActionBar?.show()
//        }
    }

    private fun initViews() {
        activity?.let {
            detailsViewModel = ViewModelProviders.of(it).get(DetailsViewModel::class.java)
        }

        val context = context
        val activity = activity

        if (context != null && activity != null) {
            locationService = LocationService(context, activity)
            locationService?.setListener(locationServiceListener)
        }

        mapFragment = childFragmentManager.findFragmentById(R.id.fragmentDetails_mapFragment) as MapFragment
    }

    private fun setupView() {
        locationService?.start()
        mapFragment.view?.isEnabled = false

    }

    private fun attachObservers() {
        detailsViewModel.selected.observe(viewLifecycleOwner, Observer {
            mushroom = it
            configureView(it)
        })

        detailsViewModel.heatMapObservationCoordinates.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Items -> {
                    mapFragment.addHeatMap(it.items)
                    }
            }
        })
    }

    private fun configureView(mushroom: Mushroom) {
        fragmentDetails_imagesView.configure(mushroom.images)
        fragmentDetails_titlesView.configure(
            mushroom.danishName?.upperCased() ?: mushroom.fullName.italized(context!!),
            (if (mushroom.danishName == null) null else mushroom.fullName.italized(context!!))
        )
        addDescriptionView("Beskrivelse", mushroom.attributes?.diagnosis?.capitalize())
        addDescriptionView("Forvekslingsmuligheder", mushroom.attributes?.similarities?.capitalize())


        fragmentDetails_informationViewHeader.text = "Information"

        var information: MutableList<Pair<String, String>> = mutableListOf()
        mushroom.totalObservations?.let { information.add(Pair("Antal danske fund:", it.toString())) }
        mushroom.lastAcceptedObservation?.let { information.add(Pair("Seneste fund:", it)) }
        mushroom.updatedAt?.let { information.add(Pair("Sidst opdateret d.:", it)) }
        fragmentDetails_informationView.configure(information)
        fragmentDetails_mapFragmentHeader.text = "Fund i nærheden"
        locationService?.start()
    }

    fun addDescriptionView(title: String?, content: String?) {
        if (content != null && content != "") {
            val descriptionView = DescriptionView(context, null)
            descriptionView.configure(title, content)
            fragmentDetails_dynamicViewsLinearLayout.addView(descriptionView)

            val space = Space(context)
            space.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (24F * (context?.getResources()?.displayMetrics?.density ?: 1F)).toInt())

            fragmentDetails_dynamicViewsLinearLayout.addView(space)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationService?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


}


