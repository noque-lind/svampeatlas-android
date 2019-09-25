package com.noque.svampeatlas.fragments


import android.annotation.SuppressLint
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
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.noque.svampeatlas.adapters.CommentsAdapter
import com.noque.svampeatlas.adapters.ObservationsAdapter
import com.noque.svampeatlas.BuildConfig
import com.noque.svampeatlas.extensions.italized
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.extensions.upperCased
import com.noque.svampeatlas.models.Image
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.Observation
import kotlinx.android.synthetic.main.fragment_details.*
import com.noque.svampeatlas.R
import com.noque.svampeatlas.services.LocationService
import com.noque.svampeatlas.utilities.Geometry
import com.noque.svampeatlas.views.DescriptionView
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.views.BackgroundView
import com.noque.svampeatlas.views.BlankActivity
import com.noque.svampeatlas.views.TitlesView
import com.noque.svampeatlas.views.ImagesView
import com.noque.svampeatlas.views.InformationView
import com.noque.svampeatlas.view_models.*
import com.noque.svampeatlas.view_models.factories.ObservationsViewModelFactory
import com.noque.svampeatlas.view_models.factories.SpeciesViewModelFactory
import java.io.File
import java.util.*


class DetailsFragment : Fragment() {

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationService?.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        val TAG = "DetailsFragment"
        val KEY_HAS_EXPANDED = "KEY_HAS_EXPANDED"
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
    private var hasExpanded = false

    private val args: DetailsFragmentArgs by navArgs()

    private var locationService: LocationService? = null

    // Views

    private var nestedScrollView: NestedScrollView? = null
    private var appBarLayout: AppBarLayout? = null
    private var collapsibleToolBarLayout: CollapsingToolbarLayout? = null
    private var toolbar: Toolbar? = null
    private var imagesView: ImagesView? = null
    private var titlesView: TitlesView? = null
    private var descriptionViewLinearLayout: LinearLayout? = null
    private var informationViewHeader: TextView? = null
    private var informationView: InformationView? = null
    private var mapFragmentHeader: TextView? = null
    private var mapFragment: MapFragment? = null
    private var recyclerViewHeader: TextView? = null
    private var recyclerView: RecyclerView? = null
    private var takesSelectionButton: Button? = null
    private var backgroundView: BackgroundView? = null

    // View models
    private val speciesViewModel by lazy {
        ViewModelProviders.of(this, SpeciesViewModelFactory(args.id, requireActivity().application)).get(SpeciesViewModel::class.java)
    }

    private val observationViewModel by lazy {
        ViewModelProviders.of(this, ObservationsViewModelFactory(args.id, requireActivity().application)).get(ObservationViewModel::class.java)
    }

    private val sessionViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(SessionViewModel::class.java)
    }

    private val newObservationViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(NewObservationViewModel::class.java)
    }

    // Adapters
    private val commentsAdapter: CommentsAdapter by lazy {
        val adapter = CommentsAdapter()

        adapter.sendCommentClicked = { comment ->
            sessionViewModel.uploadComment(observationViewModel.id, comment)
        }

        adapter
    }

    private val observationsAdapter by lazy {
        val adapter = ObservationsAdapter()

        adapter.observationClicked = {
            val action = DetailsFragmentDirections.actionGlobalMushroomDetailsFragment(
                it.id,
                TakesSelection.NO,
                Type.OBSERVATION,
                null,
                null
            )

            findNavController().navigate(action)
        }

        adapter
    }


    // Listeners
    private var locationServiceListener = object : LocationService.Listener {
        override fun requestPermission(permissions: Array<out String>, requestCode: Int) {
            requestPermissions(permissions, requestCode)
        }

        override fun locationRetrievalError(error: LocationService.Error) {
            mapFragment?.setError(
                error,
                resources.getString(R.string.locationservice_error_permissions_handler)
            ) {
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
                35000.0,
                Geometry.Type.RECTANGLE
            )

            when (args.type) {
                Type.SPECIES -> {
                    speciesViewModel.getHeatMapObservations(geometry)
                    mapFragment?.setRegion(geometry.coordinate, geometry.radius)
                }
                Type.OBSERVATION -> {
                }
            }

            locationService?.setListener(null)
            locationService = null
        }
    }

    private val mapFragmentOnClick = View.OnClickListener {
        when (args.type) {
            Type.SPECIES -> {
            }
            Type.OBSERVATION -> {
                (observationViewModel.observationState.value as? State.Items)?.items?.let {
                    val action =
                        DetailsFragmentDirections.actionMushroomDetailsFragmentToObservationLocationFragment(
                            it.coordinate.latitude.toFloat(),
                            it.coordinate.longitude.toFloat()
                        )
                    findNavController().navigate(action)
                }
            }
        }
    }

    private val takesSelectionButtonPressed = View.OnClickListener {
        when (args.takesSelection) {
            TakesSelection.SELECT -> {
                    if (args.type == Type.SPECIES) {
                        newObservationViewModel.setMushroom((speciesViewModel.mushroomState.value as? State.Items)?.items)
                        args.predictionResults?.let { newObservationViewModel.setDeterminationNotes(it) }
                        args.imageFilePath?.let { newObservationViewModel.appendImage(File(it)) }

                        val action = DetailsFragmentDirections.actionMushroomDetailsFragmentToAddObservationFragment()
                        findNavController().navigate(action)
                    }
            }

            TakesSelection.DESELECT -> {
                    newObservationViewModel.setMushroom(null)
                    findNavController().navigateUp()
            }
            TakesSelection.NO -> {}
        }
    }

    private val imagesViewOnClick = { index: Int ->
        Log.d(TAG, "WHAAAT")

        var images = mutableListOf<Image>()

        when (args.type) {
            Type.SPECIES -> {
                (speciesViewModel.mushroomState.value as? State.Items)?.items?.images?.let {
                    images.addAll(it)
                }
            }
            Type.OBSERVATION -> {
                (observationViewModel.observationState.value as? State.Items)?.items?.images?.let {
                    images.addAll(it)
                }
            }
        }

        val action = DetailsFragmentDirections.actionMushroomDetailsFragmentToImageFragment(
            index,
            images.toTypedArray()
        )
        if (findNavController().currentDestination?.id == R.id.mushroomDetailsFragment) {
            findNavController().navigate(action)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        savedInstanceState?.getBoolean(KEY_HAS_EXPANDED)?.let {
            hasExpanded = it
        }
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
        setupViewModels()
    }

    override fun onStart() {
        super.onStart()
        fetchLocationIfNeeded()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_HAS_EXPANDED, hasExpanded)
        super.onSaveInstanceState(outState)
    }

    private fun initViews() {
        nestedScrollView = detailsFragment_nestedScrollView
        appBarLayout = detailsFragment_appBarLayout
        collapsibleToolBarLayout = detailsFragment_collapsingToolbarLayout
        toolbar = detailsFragment_toolbar
        imagesView = detailsFragment_imagesView
        titlesView = detailsFragment_titlesView
        descriptionViewLinearLayout = detailsFragment_descriptionViewLinearLayout
        informationViewHeader = detailsFragment_informationViewHeader
        informationView = detailsFragment_informationView
        mapFragmentHeader = detailsFragment_mapFragmentHeader
        mapFragment =
            childFragmentManager.findFragmentById(R.id.detailsFragment_mapFragment) as MapFragment
        recyclerViewHeader = detailsFragment_recyclerViewHeader
        recyclerView = detailsFragment_recyclerView
        takesSelectionButton = detailsFragment_takesSelectionButton
        backgroundView = detailsFragment_backgroundView
    }

    private fun setupViews() {
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)

        nestedScrollView?.isNestedScrollingEnabled = false
        collapsibleToolBarLayout?.setExpandedTitleColor(Color.alpha(0))
        collapsibleToolBarLayout?.setCollapsedTitleTextColor(
            ResourcesCompat.getColor(
                resources,
                R.color.colorWhite,
                null
            )
        )

        appBarLayout?.setExpanded(false, false)



        imagesView?.setOnClickedAtIndex(imagesViewOnClick)
        mapFragment?.setOnClickListener(mapFragmentOnClick)
        nestedScrollView?.visibility = View.GONE


        when (args.takesSelection) {
            TakesSelection.SELECT -> {
                takesSelectionButton?.setOnClickListener(takesSelectionButtonPressed)
                takesSelectionButton?.setBackgroundColor(
                    ContextCompat.getColor(
                        context!!,
                        R.color.colorGreen
                    )
                )
            }

            TakesSelection.DESELECT -> {
                takesSelectionButton?.setOnClickListener(takesSelectionButtonPressed)
                takesSelectionButton?.setBackgroundColor(
                    ContextCompat.getColor(
                        context!!,
                        R.color.colorRed
                    )
                )
            }

            TakesSelection.NO -> {
                takesSelectionButton?.visibility = View.GONE
            }
        }

        when (args.type) {
            Type.OBSERVATION -> {
                recyclerView?.apply {
                    adapter = commentsAdapter
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                }

            }
            Type.SPECIES -> {
                recyclerView?.apply {
                    adapter = observationsAdapter
                    layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                }
            }
        }
    }

    private fun setupViewModels() {
        sessionViewModel.commentUploadState.observe(viewLifecycleOwner, Observer {
            backgroundView?.reset()
            when (it) {
                is State.Items -> {
                    commentsAdapter.addComment(it.items)
                }

                is State.Error -> {

                }

                is State.Loading -> {
                    backgroundView?.setLoading()
                }
            }
        })

        when (args.type) {
            Type.SPECIES -> {
                speciesViewModel.mushroomState.observe(viewLifecycleOwner, Observer {
                    backgroundView?.reset()

                    when (it) {
                        is State.Items -> {
                            configureView(it.items)
                        }
                        is State.Loading -> {
                            appBarLayout?.setExpanded(false, false)
                            backgroundView?.setLoading()
                        }
                        is State.Error -> {
                            backgroundView?.setError(it.error)
                        }
                    }
                })

                speciesViewModel.heatMapObservationCoordinates.observe(
                    viewLifecycleOwner,
                    Observer {
                        when (it) {
                            is State.Items -> {
                                mapFragment?.addHeatMap(it.items)
                            }

                            is State.Loading -> {
                                mapFragment?.setLoading()
                            }

                            is State.Error -> {
                                mapFragment?.setError(it.error, null, null)
                            }
                        }
                    })

                speciesViewModel.recentObservationsState.observe(viewLifecycleOwner, Observer {
                    when (it) {
                        is State.Items -> {
                            observationsAdapter.configure(it.items, false)
                        }
                    }
                })
            }

            Type.OBSERVATION -> {
                observationViewModel.observationState.observe(viewLifecycleOwner, Observer {
                    backgroundView?.reset()

                    when (it) {
                        is State.Items -> {
                            configureView(it.items)
                        }

                        is State.Loading -> {
                            appBarLayout?.setExpanded(false, false)
                            backgroundView?.setLoading()
                        }

                        is State.Error -> {
                            backgroundView?.setError(it.error)
                        }
                    }
                })
            }
        }
    }

    private fun prepareViewsForContent() {
        nestedScrollView?.visibility = View.VISIBLE
    }

    private fun configureView(observation: Observation) {
        prepareViewsForContent()
        fetchLocationIfNeeded()

        if (observation.images.isNotEmpty()) {
            nestedScrollView?.isNestedScrollingEnabled = true
            imagesView?.configure(observation.images)
            if (!hasExpanded) {
                appBarLayout?.setExpanded(true, true)
                hasExpanded = true
                appBarLayout?.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, i ->
                    if (i == 0) collapsibleToolBarLayout?.title = observation.speciesProperties.name
                })
            } else {
                collapsibleToolBarLayout?.title = observation.speciesProperties.name
            }

        }


        if (observation.images.count() == 0) imagesView?.visibility = View.GONE

        titlesView?.configure(
            observation.speciesProperties.name.upperCased(),
            observation.observationBy?.upperCased()
        )

        addDescriptionView(
            resources.getString(R.string.detailsFragment_ecologyNotes),
            observation.ecologyNote
        )
        addDescriptionView(resources.getString(R.string.detailsFragment_notes), observation.note)


        var information = mutableListOf<Pair<String, String>>()
        observation.date?.let {
            information.add(
                Pair(
                    "Fundets dato:",
                    it.toReadableDate(true, true)
                )
            )
        }
        informationView?.configure(information)

        observation.comments?.let {
            commentsAdapter.configure(it, sessionViewModel.isLoggedIn)
        }

        mapFragment?.setOnClickListener(mapFragmentOnClick)
        mapFragmentHeader?.setText(R.string.detailsFragment_observationLocation)
        mapFragment?.addLocationMarker(observation.coordinate)
        mapFragment?.setRegion(observation.coordinate)
    }

    @SuppressLint("DefaultLocale")
    private fun configureView(mushroom: Mushroom) {
        prepareViewsForContent()

        if (!mushroom.images.isNullOrEmpty()) {
            nestedScrollView?.isNestedScrollingEnabled = true
            imagesView?.configure(mushroom.images)

            if (!hasExpanded) {
                appBarLayout?.setExpanded(true, true)
                hasExpanded = true
                appBarLayout?.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, i ->
                    if (i == 0) collapsibleToolBarLayout?.title =
                        mushroom.danishName?.upperCased() ?: mushroom.fullName.italized()
                })
            } else {
                collapsibleToolBarLayout?.title =
                    mushroom.danishName?.upperCased() ?: mushroom.fullName.italized()
            }
        } else {
            collapsibleToolBarLayout?.title =
                mushroom.danishName?.upperCased() ?: mushroom.fullName.italized()
        }

        if (mushroom.isGenus) {
            when (args.takesSelection) {
                TakesSelection.NO -> {
                }
                TakesSelection.SELECT -> {
                    takesSelectionButton?.setText(R.string.detailsFragment_select_genus)
                }
                TakesSelection.DESELECT -> {
                    takesSelectionButton?.setText(R.string.detailsFragment_deselect_genus)
                }
            }
        } else {
            when (args.takesSelection) {
                TakesSelection.NO -> {
                }
                TakesSelection.SELECT -> {
                    takesSelectionButton?.setText(R.string.detailsFragment_select_species)
                }
                TakesSelection.DESELECT -> {
                    takesSelectionButton?.setText(R.string.detailsFragment_deselect_species)
                }
            }
        }

        titlesView?.configure(
            mushroom.danishName?.upperCased() ?: mushroom.fullName.italized(),
            (if (mushroom.danishName == null) null else mushroom.fullName.italized())
        )

        if (Locale.getDefault() == Locale.forLanguageTag("da-DK")) {
            addDescriptionView(
                getString(R.string.detailsFragment_diagnosis),
                mushroom.attributes?.diagnosis?.capitalize()
            )


            addDescriptionView(
                resources.getString(R.string.detailsFragment_similarities),
                mushroom.attributes?.similarities?.capitalize()
            )

            addDescriptionView(
                getString(R.string.detailsFragment_eatability),
                mushroom.attributes?.edibility?.capitalize()
            )
        } else {
            addDescriptionView(
                getString(
                    R.string.detailsFragment_diagnosis
                ),
                mushroom.attributes?.englishDescription?.capitalize()
            )
        }

        var information = mutableListOf<Pair<String, String>>()
        mushroom.statistics?.acceptedObservationsCount?.let {
            information.add(
                Pair(
                    getString(R.string.detailsFragment_acceptedObservations),
                    it.toString()
                )
            )
        }
        mushroom.statistics?.lastAcceptedObservationDate?.let {
            information.add(
                Pair(
                    getString(R.string.detailsFragment_latestObservation),
                    it.toReadableDate(true, true)
                )
            )
        }
        mushroom.updatedAtDate?.let {
            information.add(
                Pair(
                    getString(R.string.detailsFragment_lastUpdate),
                    it.toReadableDate(true, true)
                )
            )
        }
        informationView?.configure(information)

        mapFragmentHeader?.setText(R.string.detailsFragment_heatmap)
        recyclerViewHeader?.text = getString(R.string.detailsFragment_latest_observations)
    }

    private fun addDescriptionView(title: String?, content: String?) {
        if (content != null && content != "") {
            val descriptionView = DescriptionView(context, null)
            descriptionView.configure(title, content)
            descriptionViewLinearLayout?.addView(descriptionView)

            val space = Space(context)
            space.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (24F * (context?.getResources()?.displayMetrics?.density ?: 1F)).toInt()
            )
            descriptionViewLinearLayout?.addView(space)
        }
    }

    private fun fetchLocationIfNeeded() {
        when (args.type) {
            Type.SPECIES -> {
                if (speciesViewModel.heatMapObservationCoordinates.value == null) {
                    locationService = LocationService(requireActivity().applicationContext)
                    locationService?.setListener(locationServiceListener)
                    locationService?.start()
                }
            }
            Type.OBSERVATION -> {
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "On detach")
    }

    override fun onDestroyView() {
        recyclerView?.adapter = null
        locationService?.setListener(null)

       nestedScrollView = null
         appBarLayout = null
        collapsibleToolBarLayout = null
        toolbar = null
        imagesView = null
        titlesView = null
         descriptionViewLinearLayout = null
         informationViewHeader= null
        informationView= null
        mapFragmentHeader = null
         mapFragment = null
         recyclerViewHeader = null
         recyclerView = null
        takesSelectionButton= null
         backgroundView = null

        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d(TAG, "On destroy")
        super.onDestroy()
    }
}


