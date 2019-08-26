package com.noque.svampeatlas.View.Fragments


import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.alpha
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.noque.svampeatlas.Adapters.AddObservationAdapters.CommentsAdapter
import com.noque.svampeatlas.BuildConfig
import com.noque.svampeatlas.Extensions.italized
import com.noque.svampeatlas.Extensions.upperCased
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Model.Observation
import com.noque.svampeatlas.ViewModel.DetailsViewModel
import kotlinx.android.synthetic.main.fragment_details.*
import com.noque.svampeatlas.R
import com.noque.svampeatlas.Services.LocationService
import com.noque.svampeatlas.Utilities.Geometry
import com.noque.svampeatlas.View.DescriptionView
import com.noque.svampeatlas.Model.State
import com.noque.svampeatlas.View.BackgroundView
import com.noque.svampeatlas.View.BlankActivity
import com.noque.svampeatlas.View.TitlesView
import com.noque.svampeatlas.View.Views.ImagesView
import com.noque.svampeatlas.View.Views.InformationView
import com.noque.svampeatlas.ViewModel.MushroomsViewModel
import com.noque.svampeatlas.ViewModel.ObservationsViewModel
import com.noque.svampeatlas.ViewModel.SessionViewModel
import kotlinx.android.synthetic.main.activity_blank.*
import org.w3c.dom.Text
import java.lang.Exception
import java.lang.ref.WeakReference


class DetailsFragment : Fragment(){

    companion object {
        val TAG = "DetailsFragment"
    }

    enum class TakesSelection {
        NO,
        SELECT,
        DESELECT
    }

    enum class Type {
        SPECIES,
        OBSERVATION
    }

    // Objects
    private val args: DetailsFragmentArgs by navArgs()
    private var locationService: LocationService? = null

    private var mushroom: Mushroom? = null
    private var observation: Observation? = null

    // Views
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var collapsibleToolBarLayout: CollapsingToolbarLayout
    private lateinit var toolbar: Toolbar
    private lateinit var imagesView: ImagesView
    private lateinit var titlesView: TitlesView
    private lateinit var descriptionViewLinearLayout: LinearLayout
    private lateinit var informationView: InformationView
    private lateinit var mapFragmentHeader: TextView
    private lateinit var mapFragment: MapFragment
    private lateinit var recyclerViewHeader: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var takesSelectionButton: Button
    private lateinit var backgroundView: BackgroundView

    // View models
    private lateinit var mushroomsViewModel: MushroomsViewModel
    private lateinit var observationViewModel: ObservationsViewModel
    private lateinit var sessionViewModel: SessionViewModel

    // Adapters
    private val commentsAdapter: CommentsAdapter by lazy {
        val adapter = CommentsAdapter()

        adapter.sendCommentClicked = { comment ->
            Log.d(TAG, comment)

            observation?.let {
                sessionViewModel.uploadComment(it.id, comment)
            }
        }

        adapter
    }


    // Listeners
    private var locationServiceListener = object: LocationService.Listener {
        override fun requestPermission(permissions: Array<out String>, requestCode: Int) {
            requestPermission(permissions, requestCode)
        }

        override fun locationRetrievalError(error: LocationService.Error) {
            mapFragment.setError(error, resources.getString(R.string.locationservice_error_permissions_handler)) {
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                intent.data = uri
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }

        override fun locationRetrieved(location: Location) {
            val geometry = Geometry(
                LatLng(location.latitude, location.longitude),
                80000.0,
                Geometry.Type.RECTANGLE
            )

            when (args.type) {
                Type.SPECIES -> {
                    observationViewModel.getHeatMapObservations(args.id, geometry)
                    mapFragment.setRegion(geometry.coordinate, geometry.radius)
                }
                Type.OBSERVATION -> {}
            }
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
        setupViews()
        setupViewModels()

        when (args.type) {
            Type.SPECIES -> mushroomsViewModel.getMushroom(args.id)
            Type.OBSERVATION -> { observationViewModel.getObservation(args.id) }
        }


    }

    private fun initViews() {
        appBarLayout = detailsFragment_appBarLayout
        collapsibleToolBarLayout = detailsFragment_collapsingToolbarLayout
        toolbar = detailsFragment_toolbar
        imagesView = detailsFragment_imagesView
        titlesView = detailsFragment_titlesView
        descriptionViewLinearLayout = detailsFragment_descriptionViewLinearLayout
        informationView = detailsFragment_informationView
        mapFragmentHeader = detailsFragment_mapFragmentHeader
        mapFragment = childFragmentManager.findFragmentById(R.id.detailsFragment_mapFragment) as MapFragment
        recyclerViewHeader = detailsFragment_recyclerViewHeader
        recyclerView = detailsFragment_recyclerView
        takesSelectionButton = detailsFragment_takesSelectionButton
        backgroundView = detailsFragment_backgroundView
    }

    private fun setupViews() {
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
        collapsibleToolBarLayout.setExpandedTitleColor(Color.alpha(0))
        collapsibleToolBarLayout.setCollapsedTitleTextColor(ResourcesCompat.getColor(resources, R.color.colorWhite, null))

        when (args.takesSelection) {
            TakesSelection.SELECT -> {
                takesSelectionButton.setText(R.string.takesSelectionButton_select)
                takesSelectionButton.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorGreen))
                takesSelectionButton.setOnClickListener {
                    mushroomsViewModel.setMushroom(mushroom)
                    findNavController().navigateUp()
                }
            }

            TakesSelection.DESELECT -> {
                takesSelectionButton.setText(R.string.takesSelectionButton_deselect)
                takesSelectionButton.setBackgroundColor(ContextCompat.getColor(context!!, R.color.colorRed))
                takesSelectionButton.setOnClickListener {
                    mushroomsViewModel.setMushroom(null)
                    findNavController().navigateUp()
                }
            }

            TakesSelection.NO -> {
                takesSelectionButton.visibility = View.GONE
            }
        }

        when (args.type) {
            Type.OBSERVATION -> {
                recyclerView.apply {
                    adapter = commentsAdapter
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                }

            }
            Type.SPECIES -> {}
        }
    }

    private fun setupViewModels() {
        activity?.let {
            mushroomsViewModel = ViewModelProviders.of(activity!!).get(MushroomsViewModel::class.java)
            mushroomsViewModel.mushroomState.observe(viewLifecycleOwner, Observer {
                backgroundView.reset()

                when (it) {
                    is State.Items -> {
                        mushroom = it.items
                        configureView(it.items) }

                    is State.Loading -> { backgroundView.setLoading() }
                }
            })


            observationViewModel = ViewModelProviders.of(this).get(ObservationsViewModel::class.java)
            observationViewModel.observationState.observe(viewLifecycleOwner, Observer {
                backgroundView.reset()

                when (it) {
                    is State.Items -> {
                        observation = it.items
                        configureView(it.items)
                    }

                    is State.Loading -> { backgroundView.setLoading() }
                }
            })

            observationViewModel.heatMapObservationCoordinates.observe(viewLifecycleOwner, Observer {
                when (it) {
                    is State.Items -> {
                        mapFragment.addHeatMap(it.items)
                    }

                    is State.Loading -> {
                        mapFragment.setLoading()
                    }

                    is State.Error -> {
                        mapFragment.setError(it.error, null, null)
                    }
                }
            })

            sessionViewModel = ViewModelProviders.of(requireActivity()).get(SessionViewModel::class.java)

            sessionViewModel.commentUploadState.observe(viewLifecycleOwner, Observer {
                backgroundView.reset()
                when (it) {
                    is State.Items -> {
                        commentsAdapter?.addComment(it.items)
                    }

                    is State.Error -> {

                    }

                    is State.Loading -> {
                        backgroundView.setLoading()
                    }
                }
            })

        }
    }


    private fun configureView(observation: Observation) {
        if (observation.images.isEmpty()) appBarLayout.setExpanded(false, false)
        collapsibleToolBarLayout.title = observation.speciesProperties.name


        imagesView.configure(observation.images)
        if (observation.images.count() == 0) imagesView.visibility = View.GONE

        titlesView.configure(
            observation.speciesProperties.name.upperCased(),
            observation.observationBy?.upperCased()
        )

        addDescriptionView(resources.getString(R.string.detailsFragment_ecologyNotes), observation.ecologyNote)
        addDescriptionView(resources.getString(R.string.detailsFragment_notes), observation.note)


        var information = mutableListOf<Pair<String, String>>()
        observation.date?.let { information.add(Pair("Fundets dato:", it.toString())) }
        informationView.configure(information)

        observation.comments?.let {
            commentsAdapter?.configure(it, sessionViewModel.isLoggedIn)
        }

        mapFragmentHeader.setText(R.string.detailsFragment_observationLocation)
        mapFragment.addLocationMarker(observation.coordinate)
        mapFragment.setRegion(observation.coordinate)
    }

    private fun configureView(mushroom: Mushroom) {
        locationService = LocationService(requireContext(), requireActivity())
        locationService?.setListener(locationServiceListener)
        locationService?.start()


        if (mushroom.images.isEmpty()) appBarLayout.setExpanded(false, false)
        imagesView.configure(mushroom.images)

        titlesView.configure(
            mushroom.danishName?.upperCased() ?: mushroom.fullName.italized(),
            (if (mushroom.danishName == null) null else mushroom.fullName.italized())
        )

        addDescriptionView(resources.getString(R.string.detailsFragment_diagnosis), mushroom.attributes?.diagnosis?.capitalize())
        addDescriptionView(resources.getString(R.string.detailsFragment_similarities), mushroom.attributes?.similarities?.capitalize())

        var information = mutableListOf<Pair<String, String>>()
        mushroom.totalObservations?.let { information.add(Pair("Antal danske fund:", it.toString())) }
        mushroom.lastAcceptedObservation?.let { information.add(Pair("Seneste fund:", it)) }
        mushroom.updatedAt?.let { information.add(Pair("Sidst opdateret d.:", it)) }
        informationView.configure(information)

        mapFragmentHeader.setText(R.string.detailsFragment_heatmap)
        mapFragment.setLoading()
    }

    fun addDescriptionView(title: String?, content: String?) {
        if (content != null && content != "") {
            val descriptionView = DescriptionView(context, null)
            descriptionView.configure(title, content)
            descriptionViewLinearLayout.addView(descriptionView)

            val space = Space(context)
            space.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (24F * (context?.getResources()?.displayMetrics?.density ?: 1F)).toInt())
            descriptionViewLinearLayout.addView(space)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationService?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        mushroomsViewModel.clearData()
    }
}


