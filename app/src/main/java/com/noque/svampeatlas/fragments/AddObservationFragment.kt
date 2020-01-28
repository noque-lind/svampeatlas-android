package com.noque.svampeatlas.fragments

import android.content.pm.ActivityInfo
import android.graphics.*
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.tabs.TabLayout
import com.noque.svampeatlas.adapters.add_observation.AddImagesAdapter
import com.noque.svampeatlas.adapters.add_observation.InformationAdapter
import com.noque.svampeatlas.extensions.changeColor
import com.noque.svampeatlas.extensions.pxToDp
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.R
import com.noque.svampeatlas.services.LocationService
import com.noque.svampeatlas.utilities.DispatchGroup
import com.noque.svampeatlas.views.BlankActivity
import com.noque.svampeatlas.views.SpinnerView
import com.noque.svampeatlas.view_holders.AddImageViewHolder
import com.noque.svampeatlas.view_models.NewObservationViewModel
import com.noque.svampeatlas.view_models.SessionViewModel
import kotlinx.android.synthetic.main.custom_toast.*
import kotlinx.android.synthetic.main.custom_toast.view.*
import kotlinx.android.synthetic.main.fragment_add_observation.*
import java.util.*
import kotlin.concurrent.timerTask


class AddObservationFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        locationService.onRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        val TAG = "AddObservationFragment"
        val KEY_LOCALITY_ID_SHOWN = "KEY_LOCALITY_ID_SHOWN"
        val KEY_ADDIMAGE_SHOWN = "KEY_ADDIMAGE_SHOWN"
        val DEFAULT_LOCALITY_ID = 0
        val LOCALITY_ID_FOR_ERROR = 1921

    }

    enum class Type {
        NEWOBSERVATION,
        IDENTIFYEDOBSERVATION
    }

    enum class Category {
        SPECIES,
        DETAILS,
        LOCALITY;

        companion object {
            val values = values()
        }
    }

    // Objects
    private val args: AddObservationFragmentArgs by navArgs()

    private var localityIdShown = DEFAULT_LOCALITY_ID
    private var addImageShown = false

    private val locationService by lazy {
        LocationService(requireActivity().applicationContext)
    }

    private var toast: Toast? = null

    // Views

    private var viewPager: ViewPager? = null
    private var addImagesRecyclerView: RecyclerView? = null
    private var tabLayout: TabLayout? = null
    private var spinnerView: SpinnerView? = null

    // View models
    private val newObservationViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(NewObservationViewModel::class.java)
    }

    private val sessionViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(SessionViewModel::class.java)
    }


    // Adapters
    private val addImagesAdapter by lazy {
        val adapter = AddImagesAdapter()

        adapter.addImageButtonClicked = {
            val action =
                AddObservationFragmentDirections.actionGlobalCameraFragment()
            action.type = CameraFragment.Type.IMAGECAPTURE
            findNavController().navigate(action)
        }

        adapter
    }

    private val informationAdapter by lazy {
        InformationAdapter(
            context,
            Category.values,
            childFragmentManager,
            FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
        )
    }

    // Listeners

    private val locationServiceListener = object : LocationService.Listener {
        override fun requestPermission(permissions: Array<out String>, requestCode: Int) {
            requestPermissions(permissions, requestCode)
        }

        override fun locationRetrievalError(error: LocationService.Error) {
            newObservationViewModel.setLocationError(error)
        }

        override fun locationRetrieved(location: Location) {
            newObservationViewModel.setCoordinate(LatLng(location.latitude, location.longitude), location.accuracy)
        }

    }

    private val imageSwipedCallback =
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.UP) {
            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (viewHolder is AddImageViewHolder) {
                    return 0
                } else {
                    return super.getSwipeDirs(recyclerView, viewHolder)
                }

            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {

                val trashIcon = resources.getDrawable(R.drawable.ic_delete_black_24dp, null)
                trashIcon.bounds = Rect(
                    viewHolder.itemView.left + (viewHolder.itemView.width / 2) - (trashIcon.intrinsicWidth / 2),
                    ((viewHolder.itemView.height) / 2) - (trashIcon.intrinsicHeight / 2) + recyclerView.paddingTop,
                    viewHolder.itemView.right - (viewHolder.itemView.width / 2) + (trashIcon.intrinsicWidth / 2),
                    (viewHolder.itemView.height / 2) + (trashIcon.intrinsicHeight / 2) + recyclerView.paddingTop
                )

                viewHolder.itemView.alpha = 1 - (-(dY) / viewHolder.itemView.height)

                trashIcon.draw(c)
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }


            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                newObservationViewModel.removeImageAt(position)
                addImagesAdapter.notifyItemRemoved(viewHolder.adapterPosition)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addImageShown = savedInstanceState?.getBoolean(KEY_ADDIMAGE_SHOWN) ?: false
        localityIdShown = savedInstanceState?.getInt(KEY_LOCALITY_ID_SHOWN) ?: DEFAULT_LOCALITY_ID
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (!addImageShown && args.type == Type.NEWOBSERVATION) {
            addImageShown = true
            val action =
                AddObservationFragmentDirections.actionAddObservationFragmentToCameraFragment()
            action.type = CameraFragment.Type.NEWOBSERVATION
            findNavController().navigate(action)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        return inflater.inflate(R.layout.fragment_add_observation, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupView()
        setupViewModels()

        // Figure out if only activity is needed
        locationService.setListener(locationServiceListener)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.add_observation_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_addObservationFragment_uploadButton -> {
                uploadButtonPressed()
            }
            R.id.menu_addObservationFragment_resetButton -> {
                reset()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_LOCALITY_ID_SHOWN, localityIdShown)
        outState.putBoolean(KEY_ADDIMAGE_SHOWN, addImageShown)
        super.onSaveInstanceState(outState)
    }

    private fun initViews() {
        viewPager = addObservationFragment_viewPager
        addImagesRecyclerView = addObservationFragment_addObservationImagesRecyclerView
        tabLayout = addObservationFragment_tabLayout
        spinnerView = addObservationFragment_spinner
    }

    private fun setupView() {
        (requireActivity() as BlankActivity).setSupportActionBar(addObservationFragment_toolbar)

        tabLayout?.setupWithViewPager(viewPager)

        addImagesRecyclerView?.apply {
            val myHelper = ItemTouchHelper(imageSwipedCallback)
            myHelper.attachToRecyclerView(this)

            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = RecyclerView.HORIZONTAL
            this.layoutManager = layoutManager
            this.adapter = addImagesAdapter
        }

        viewPager.apply {
            this?.adapter = informationAdapter
        }
    }

    private fun setupViewModels() {
        sessionViewModel.observationUploadState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Items -> {
                    spinnerView?.stopLoading()

                    val bitmap = BitmapFactory.decodeResource(
                        resources,
                        R.drawable.icon_elmessageview_success
                    )
                    createToast(
                        resources.getString(R.string.addObservationFragment_completedUpload_title),
                        "ID: ${it.items.first}",
                        bitmap.changeColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.colorGreen,
                                null
                            )
                        )
                    )
                    reset()
                }

                is State.Loading -> {
                    spinnerView?.startLoading()
                }

                is State.Error -> {
                    spinnerView?.stopLoading()

                    val bitmap = BitmapFactory.decodeResource(
                        resources,
                        R.drawable.icon_elmessageview_failure
                    )
                    createToast(
                        it.error.title,
                        it.error.message,
                        bitmap.changeColor(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.colorRed,
                                null
                            )
                        )
                    )

                    sessionViewModel.resetObservationUploadState()
                }
            }
        })


        newObservationViewModel.images.observe(viewLifecycleOwner, Observer {
            addImagesAdapter.configure(it)
        })

        newObservationViewModel.coordinate.observe(viewLifecycleOwner, Observer {
            if (it == null) {
                locationService.start()
            }
        })

        newObservationViewModel.locality.observe(viewLifecycleOwner, Observer {
            if (viewPager?.currentItem == Category.LOCALITY.ordinal) {
                localityIdShown = it?.id ?: DEFAULT_LOCALITY_ID
            } else {
                it?.let {
                    if (it.id != localityIdShown) {
                        localityIdShown = it.id
                        val bitmap =
                            BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin)
                        createToast(
                            resources.getString(R.string.addObservationFragment_localityFound_title),
                            "${resources.getString(R.string.addObservationFragment_localityFound_message)}${it.name}",
                            bitmap.changeColor(
                                ResourcesCompat.getColor(
                                    resources,
                                    R.color.colorGreen,
                                    null
                                )
                            )
                        )
                    }
                }
            }
        })

        newObservationViewModel.localityState.observe(viewLifecycleOwner, Observer { state ->
            if (viewPager?.currentItem == Category.LOCALITY.ordinal) return@Observer

            when (state) {
                is State.Error -> {
                    if (localityIdShown != LOCALITY_ID_FOR_ERROR) {
                        localityIdShown = LOCALITY_ID_FOR_ERROR
                        val bitmap =
                            BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin)
                        createToast(
                            state.error.title,
                            state.error.message,
                            bitmap.changeColor(
                                ResourcesCompat.getColor(
                                    resources,
                                    R.color.colorRed,
                                    null
                                )
                            )
                        )
                    }
                }
            }
        })
}

    private fun uploadButtonPressed() {
        val result = newObservationViewModel.prepareForUpload(sessionViewModel.user.value?.id ?: 0)

        result.onError {
            when (it) {
                is NewObservationViewModel.Error.NoLocationDataError -> {
                    viewPager?.currentItem = Category.LOCALITY.ordinal
                }

                is NewObservationViewModel.Error.NoSubstrateError -> {
                    viewPager?.currentItem = Category.DETAILS.ordinal
                }

                is NewObservationViewModel.Error.NoVegetationTypeError -> {
                    viewPager?.currentItem = Category.DETAILS.ordinal
                }

                is NewObservationViewModel.Error.NoMushroomError -> {
                    viewPager?.currentItem = Category.SPECIES.ordinal
                }
            }

            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_elmessageview_failure)
            createToast(it.title, it.message, bitmap.changeColor(ResourcesCompat.getColor(resources, R.color.colorRed, null)))
        }


        result.onSuccess {
            sessionViewModel.uploadObservation(it.first, it.second)
        }
    }

    private fun createToast(title: String, message: String, bitmap: Bitmap) {
       toast?.cancel()
        toast = null


            val container = custom_toast_container
            val layout = layoutInflater.inflate(R.layout.custom_toast, container)

            layout.customToast_titleTextView.text = title
            layout.customToast_messageTextView.text = message
            layout.customToast_imageView.setImageBitmap(bitmap)
            with (Toast(context)) {
                    setGravity(Gravity.BOTTOM, 0, 16.pxToDp(context))
                    duration = Toast.LENGTH_SHORT
                    view = layout
                    show()
                    toast = this
                }
            }

    private fun reset() {
        viewPager?.currentItem = Category.SPECIES.ordinal
        if (localityIdShown == LOCALITY_ID_FOR_ERROR) localityIdShown = DEFAULT_LOCALITY_ID
        newObservationViewModel.reset()
        sessionViewModel.resetObservationUploadState()
    }

    override fun onDestroyView() {
        locationService.setListener(null)
        addImagesRecyclerView?.adapter = null
        viewPager?.adapter = null

        addImagesRecyclerView = null
        viewPager = null
        tabLayout = null
        spinnerView = null

        super.onDestroyView()
    }
}