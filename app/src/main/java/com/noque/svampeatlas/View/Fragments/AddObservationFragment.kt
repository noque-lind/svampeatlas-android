package com.noque.svampeatlas.View.Fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.tabs.TabLayout
import com.noque.svampeatlas.Adapters.AddObservationAdapters.AddImagesAdapter
import com.noque.svampeatlas.Adapters.AddObservationAdapters.InformationAdapter
import com.noque.svampeatlas.BuildConfig
import com.noque.svampeatlas.Extensions.changeColor
import com.noque.svampeatlas.Extensions.pxToDp
import com.noque.svampeatlas.Model.State
import com.noque.svampeatlas.R
import com.noque.svampeatlas.Services.LocationService
import com.noque.svampeatlas.View.BlankActivity
import com.noque.svampeatlas.View.Views.SpinnerView
import com.noque.svampeatlas.ViewModel.NewObservationViewModel
import com.noque.svampeatlas.ViewModel.SessionViewModel
import kotlinx.android.synthetic.main.custom_toast.*
import kotlinx.android.synthetic.main.custom_toast.view.*
import kotlinx.android.synthetic.main.fragment_add_observation.*


class AddObservationFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        locationService?.onRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        val TAG = "AddObservationFragment"
        val KEY_LOCALITY_ID_SHOWN = "KEY_LOCALITY_ID_SHOWN"
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

    private var localityIdShown = 0
    private var locationService: LocationService? = null

    // Views
    private var viewPager: ViewPager? = null
    private var addImagesRecyclerView: RecyclerView? = null
    private var tabLayout: TabLayout? = null
    private var spinnerView: SpinnerView? = null

    // View models
    private lateinit var newObservationViewModel: NewObservationViewModel
    private lateinit var sessionViewModel: SessionViewModel


    // Adapters
    private val addImagesAdapter by lazy {
        val adapter = AddImagesAdapter()

        adapter.addImageButtonClicked = {
            val action =
                AddObservationFragmentDirections.actionGlobalCameraFragment(CameraFragment.Type.IMAGECAPTURE)
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
            newObservationViewModel.setCoordinate(LatLng(location.latitude, location.longitude))
            newObservationViewModel.getLocalities(LatLng(location.latitude, location.longitude))
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        val localityIdShown = savedInstanceState?.getInt(KEY_LOCALITY_ID_SHOWN)
        if (localityIdShown != null) {
            this.localityIdShown = localityIdShown
        }
        return inflater.inflate(R.layout.fragment_add_observation, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupView()
        setupViewModels()


        // Figure out if only activity is needed
        locationService = LocationService(context, activity)
        locationService?.setListener(locationServiceListener)
    }


    override fun onStart() {
        if (newObservationViewModel.locality.value == null) {
            locationService?.start()
        }
        super.onStart()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.add_observation_fragment_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_addObservationFragment_uploadButton -> { uploadButtonPressed() }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_LOCALITY_ID_SHOWN, localityIdShown)
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
        sessionViewModel = ViewModelProviders.of(requireActivity()).get(SessionViewModel::class.java)


        activity?.let {

            newObservationViewModel = ViewModelProviders.of(activity!!).get(NewObservationViewModel::class.java)

            newObservationViewModel.images.observe(viewLifecycleOwner, Observer {
                addImagesAdapter.configure(it.toList())
            })

            newObservationViewModel.coordinate.observe(viewLifecycleOwner, Observer {
                if (it == null) {
                    locationService?.start()
                }
            })

            newObservationViewModel.locality.observe(viewLifecycleOwner, Observer {
                if (viewPager?.currentItem == Category.LOCALITY.ordinal) return@Observer
                it?.let {
                    if (it.id != localityIdShown) {
                        localityIdShown = it.id
                        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin)
                        createToast(
                            resources.getString(R.string.addObservationFragment_localityFound_title),
                            "${resources.getString(R.string.addObservationFragment_localityFound_message)}${it.name}",
                            bitmap.changeColor(ResourcesCompat.getColor(resources, R.color.colorGreen, null)))
                    }
                }
            })

            newObservationViewModel.localityState.observe(viewLifecycleOwner, Observer { state ->
                if (viewPager?.currentItem == Category.LOCALITY.ordinal) return@Observer

                    when (state) {
                        is State.Error -> {
                            if (localityIdShown != -1) {
                                localityIdShown = -1
                                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin)
                                createToast(state.error.title,
                                    state.error.message,
                                    bitmap.changeColor(ResourcesCompat.getColor(resources, R.color.colorRed, null)))
                            }
                        }
                    }
            })

            newObservationViewModel.uploadState.observe(viewLifecycleOwner, Observer {
                when (it) {
                    is State.Items -> {
                        spinnerView?.stopLoading()

                        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_elmessageview_success)
                        createToast(
                            resources.getString(R.string.addObservationFragment_completedUpload_title),
                            "ID: ${it.items.first}",
                            bitmap.changeColor(ResourcesCompat.getColor(resources, R.color.colorGreen, null)))

                        viewPager?.currentItem = Category.SPECIES.ordinal
                        localityIdShown = 0
                        newObservationViewModel.reset()
                    }

                    is State.Loading -> {
                        spinnerView?.startLoading()
                    }

                    is State.Error -> {
                        spinnerView?.stopLoading()

                        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_elmessageview_failure)
                        createToast(
                            it.error.title,
                            it.error.message,
                            bitmap.changeColor(ResourcesCompat.getColor(resources, R.color.colorRed, null)))
                    }
                }
            })
        }
    }

    private fun uploadButtonPressed() {
        val user = sessionViewModel.user.value

        if (user == null) {
            // Create toast notifying user that an unexpected error happened, and that they need to login again
            findNavController().navigate(R.id.loginFragment)
        } else {
            newObservationViewModel.upload(user)?.let {

                when (it) {
                    is NewObservationViewModel.NewObservationViewModelError.NoLocationDataError -> {
                        viewPager?.currentItem = Category.LOCALITY.ordinal
                    }

                    is NewObservationViewModel.NewObservationViewModelError.NoSubstrateError -> {
                        viewPager?.currentItem = Category.DETAILS.ordinal
                    }

                    is NewObservationViewModel.NewObservationViewModelError.NoVegetationTypeError -> {
                        viewPager?.currentItem = Category.DETAILS.ordinal
                    }

                    is NewObservationViewModel.NewObservationViewModelError.NoMushroomError -> {
                        viewPager?.currentItem = Category.SPECIES.ordinal
                    }
                }

                val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_elmessageview_failure)
                createToast(it.title, it.message, bitmap.changeColor(ResourcesCompat.getColor(resources, R.color.colorRed, null)))
            }
        }
    }

    private fun createToast(title: String, message: String, bitmap: Bitmap) {
        val container = custom_toast_container
        val layout = layoutInflater.inflate(R.layout.custom_toast, container)

        layout.customToast_titleTextView.text = title
        layout.customToast_messageTextView.text = message
        layout.customToast_imageView.setImageBitmap(bitmap)

        with (Toast(context)) {
            setGravity(Gravity.BOTTOM, 0, 16.pxToDp(context))
            duration = Toast.LENGTH_LONG
            view = layout
            show()
        }
    }

    override fun onDestroyView() {
        Log.d(TAG, "On destory view")
        super.onDestroyView()
        addImagesRecyclerView = null
        viewPager = null
        tabLayout = null
        spinnerView = null
        locationService = null
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "On detach")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "on destory")
    }
}