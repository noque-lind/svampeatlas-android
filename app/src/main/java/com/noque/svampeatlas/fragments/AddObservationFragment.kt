package com.noque.svampeatlas.fragments

import android.content.pm.ActivityInfo
import android.graphics.*
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
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
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.*
import com.noque.svampeatlas.services.LocationService
import com.noque.svampeatlas.utilities.MyApplication
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.views.BlankActivity
import com.noque.svampeatlas.views.SpinnerView
import com.noque.svampeatlas.view_holders.AddImageViewHolder
import com.noque.svampeatlas.view_holders.AddedImageViewHolder
import com.noque.svampeatlas.view_models.NewObservationViewModel
import com.noque.svampeatlas.view_models.Session
import com.noque.svampeatlas.view_models.factories.NewObservationViewModelFactory
import kotlinx.android.synthetic.main.action_view_add_notebook_entry.view.*
import kotlinx.android.synthetic.main.action_view_save_notebook_entry.view.*
import kotlinx.android.synthetic.main.custom_toast.*
import kotlinx.android.synthetic.main.custom_toast.view.*
import kotlinx.android.synthetic.main.fragment_add_observation.*
import java.io.File
import java.util.*


class AddObservationFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback, PromptFragment.Listener {

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        locationService.onRequestPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val KEY_LOCALITY_ID_SHOWN = "KEY_LOCALITY_ID_SHOWN"
        private const val KEY_ADDIMAGE_SHOWN = "KEY_ADDIMAGE_SHOWN"
        private const val DEFAULT_LOCALITY_ID = 0
        private const val LOCALITY_ID_FOR_ERROR = 1921
        const val SAVED_STATE_FILE_PATH = "SAVED_STATE_FILE_PATH"
        const val SAVED_STATE_TAXON_ID = "SAVED_STATE_TAXON_ID"
    }

    enum class Type {
        New,
        FromRecognition,
        Edit,
        Note,
        EditNote;
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
    private var spinnerView by autoCleared<SpinnerView>()
    private var toolbar by autoCleared<Toolbar>()
    private var viewPager by autoCleared<ViewPager> { it?.adapter = null }
    private var addImagesRecyclerView by autoCleared<RecyclerView> { it?.adapter = null }
    private var tabLayout by autoCleared<TabLayout>() { it?.setupWithViewPager(null) }

    // View models
    private val newObservationViewModel by viewModels<NewObservationViewModel> { NewObservationViewModelFactory(args.type, args.id, args.mushroomId, args.imageFilePath,  requireActivity().application) }

    // Adapters
    private val addImagesAdapter by lazy {
        val adapter = AddImagesAdapter()

        adapter.addImageButtonClicked = {
            val action =
                AddObservationFragmentDirections.actionGlobalCameraFragment()
            action.type = CameraFragment.Type.IMAGE_CAPTURE
            findNavController().navigate(action)
        }

        adapter
    }

    private val informationAdapter by lazy {
        if (args.type == Type.Edit) {
            InformationAdapter(
                context,
                arrayOf(Category.DETAILS, Category.LOCALITY),
                childFragmentManager,
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
            )
        } else {
            InformationAdapter(
                context,
                Category.values,
                childFragmentManager,
                FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT
            )
        }
    }

    // Listeners

    override fun positiveButtonPressed() {
        newObservationViewModel.promptPositive()
//        (newObservationViewModel.useImageLocationPromptState.value as? State.Items)?.items?.first?.let {
//            newObservationViewModel.setCoordinateState(State.Items(it))
//        }
    }

    override fun negativeButtonPressed() {
        newObservationViewModel.promptNegative()
//        (newObservationViewModel.useImageLocationPromptState.value as? State.Items)?.items?.second?.let {
//            newObservationViewModel.setCoordinateState(State.Items(it))
//        }
    }

    private val locationServiceListener = object : LocationService.Listener {
        override fun requestPermission(permissions: Array<out String>, requestCode: Int) {
            requestPermissions(permissions, requestCode)
        }

        override fun isLocating() {
            newObservationViewModel.setCoordinateState(State.Loading())
        }

        override fun locationRetrievalError(error: LocationService.Error) {
            newObservationViewModel.setCoordinateState(State.Error(error))
        }

        override fun locationRetrieved(location: Location) {
            newObservationViewModel.setCoordinateState(State.Items(com.noque.svampeatlas.models.Location(
                Date(), LatLng(location.latitude, location.longitude), location.accuracy)))
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
                } else if (viewHolder is AddedImageViewHolder) {
                     if (viewHolder.isLocked) return 0 else return super.getSwipeDirs(recyclerView, viewHolder)
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

                val trashIcon = ResourcesCompat.getDrawable(resources, R.drawable.ic_delete_black_24dp, null)?.apply {
                    bounds = Rect(
                        viewHolder.itemView.left + (viewHolder.itemView.width / 2) - (intrinsicWidth / 2),
                        ((viewHolder.itemView.height) / 2) - (intrinsicHeight / 2) + recyclerView.paddingTop,
                        viewHolder.itemView.right - (viewHolder.itemView.width / 2) + (intrinsicWidth / 2),
                        (viewHolder.itemView.height / 2) + (intrinsicHeight / 2) + recyclerView.paddingTop
                    )
                }

                viewHolder.itemView.alpha = 1 - (-(dY) / viewHolder.itemView.height)

                trashIcon?.draw(c)
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
                if (newObservationViewModel.images.value?.getOrNull(position) is NewObservationViewModel.Image.New) {
                    newObservationViewModel.removeImageAt(position)
                } else if (!SharedPreferences.hasSeenImageDeletion) {
                    addImagesAdapter.notifyItemChanged(viewHolder.adapterPosition)
                    val dialog = TermsFragment()
                    dialog.arguments = Bundle().apply { putSerializable(TermsFragment.KEY_TYPE, TermsFragment.Type.IMAGEDELETIONS) }
                    dialog.show(childFragmentManager, null)
                } else {
                    newObservationViewModel.removeImageAt(position)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        addImageShown = savedInstanceState?.getBoolean(KEY_ADDIMAGE_SHOWN) ?: false
        localityIdShown = savedInstanceState?.getInt(KEY_LOCALITY_ID_SHOWN) ?: DEFAULT_LOCALITY_ID
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return if (!addImageShown && args.type == Type.New) {
            addImageShown = true
            val action =
                AddObservationFragmentDirections.actionAddObservationFragmentToCameraFragment()
            action.type = CameraFragment.Type.NEW_OBSERVATION
            findNavController().navigate(action)
            null
        } else {
            inflater.inflate(R.layout.fragment_add_observation, container, false)
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = addObservationFragment_toolbar
        viewPager = addObservationFragment_viewPager
        addImagesRecyclerView = addObservationFragment_addObservationImagesRecyclerView
        tabLayout = addObservationFragment_tabLayout
        spinnerView = addObservationFragment_spinner

        setupView()
        setupViewModels()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        when (args.type) {
            Type.New -> inflater.inflate(R.menu.add_observation_fragment_menu, menu)
            Type.Edit -> {
                inflater.inflate(R.menu.add_observation_fragment_menu, menu)
                menu.findItem(R.id.menu_addObservationFragment_overflow).isVisible = false
            }
            Type.Note -> {
                inflater.inflate(R.menu.add_observation_fragment_menu_notes, menu)
                menu.findItem(R.id.menu_addObservationFragment_note_save)?.let {
                    (it.actionView as? LinearLayout)?.apply {
                        actionView_saveNotebookEntry.setOnClickListener {
                            newObservationViewModel.saveToLocal()
                        }
                    }
                }
            }
            Type.EditNote -> {
                inflater.inflate(R.menu.menu_add_observation_menu_edit_note, menu)
                menu.findItem(R.id.menu_addObservationFragment_note_save)?.let {
                    (it.actionView as? LinearLayout)?.apply {
                        actionView_saveNotebookEntry.setOnClickListener {
                            newObservationViewModel.saveToLocal()
                        }
                    }
                }
            }
        }


//        menu.findItem(R.id.addObser)?.let {
//            (it.actionView as? LinearLayout)?.apply {
//                actionView_addNotebookEntry.setOnClickListener {
//                    val action = NotesFragmentDirections.actionNotesFragmentToAddObservationFragment().setType(AddObservationFragment.Type.Note)
//                    findNavController().navigate(action)
//                }
//            }
//        }
        super.onCreateOptionsMenu(menu, inflater)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_addObservationFragment_uploadButton -> {
                if (Type.Note == args.type) {
                    newObservationViewModel.saveToLocal()
                } else {
                    uploadButtonPressed()
                }


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

    private fun setupView() {
        if (args.type == Type.Edit) {
            toolbar.setTitle(R.string.addObservationVC_title_edit)
            toolbar.subtitle = "ID: ${args.id}"
        } else if (args.type == Type.Note) {
            toolbar.setTitle(R.string.addObservationFragment_title_note)
        } else if (args.type == Type.EditNote) {
            toolbar.setTitle(R.string.addObservationVC_title_edit_note)
            toolbar.subtitle = Date(args.id).toReadableDate(false, ignoreTime = false)
        } else {
            toolbar.setNavigationIcon(R.drawable.icon_menu_button)
            toolbar.setTitle(R.string.addObservationVC_title)
        }
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
        tabLayout.setupWithViewPager(viewPager)
        addImagesRecyclerView.apply {
            val myHelper = ItemTouchHelper(imageSwipedCallback)
            myHelper.attachToRecyclerView(this)

            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = RecyclerView.HORIZONTAL
            this.layoutManager = layoutManager
            this.adapter = addImagesAdapter
        }

        viewPager.apply {
            adapter = informationAdapter
        }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<String>(SAVED_STATE_FILE_PATH)?.observe(viewLifecycleOwner, Observer {
            newObservationViewModel.appendImage(File(it))
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<String>(SAVED_STATE_FILE_PATH)
        })

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Int>(SAVED_STATE_TAXON_ID)?.observe(viewLifecycleOwner, Observer {
            newObservationViewModel.setMushroom(it)
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<Int>(SAVED_STATE_TAXON_ID)
        })
    }

    private fun setupViewModels() {
        Session.observationUploadState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Items -> {
                    if (args.type == Type.Edit) {
                        newObservationViewModel.setupAsNew()
                        val action = AddObservationFragmentDirections.actionGlobalMyPageFragment()
                        findNavController().navigate(action)
                    } else {
                        spinnerView.stopLoading()

                        val bitmap = BitmapFactory.decodeResource(
                            resources,
                            R.drawable.icon_elmessageview_success
                        )
                        createToast(
                            resources.getString(R.string.prompt_successRecordCreation_title),
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
                }

                is State.Loading -> {
                    spinnerView.startLoading()
                }

                is State.Error -> {
                    spinnerView.stopLoading()
                }
            }
        })

        newObservationViewModel.saveLocallyState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Items -> {
                    findNavController().previousBackStackEntry?.savedStateHandle?.set(NotesFragment.RELOAD_DATA_KEY, true)
                    findNavController().navigateUp()
                }
                is State.Empty -> {}
                is State.Loading -> spinnerView.startLoading()
                is State.Error -> createToast(it.error.title, it.error.message, BitmapFactory.decodeResource(
                    resources,
                    R.drawable.icon_elmessageview_failure
                ).changeColor(R.color.colorRed))
            }
        })

        newObservationViewModel.images.observe(viewLifecycleOwner, Observer {
            addImagesAdapter.configure(it ?: mutableListOf())
        })


        newObservationViewModel.removedImage.observe(viewLifecycleOwner, Observer {
            addImagesAdapter.removeImage(it)
        })

        newObservationViewModel.coordinateState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Items, is State.Error -> {
                    tabLayout.getTabAt(Category.LOCALITY.ordinal)?.setCustomView(null)
                }
                is State.Loading -> {
                    tabLayout.getTabAt(Category.LOCALITY.ordinal)?.setCustomView(R.layout.view_spinner_small)
                }
                is State.Empty -> {
                    locationService.setListener(locationServiceListener)
                    locationService.start()
                }
            }
        })

         newObservationViewModel.localitiesState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Items, is State.Empty, is State.Error ->  tabLayout.getTabAt(Category.LOCALITY.ordinal)?.setCustomView(null)
                is State.Loading ->   tabLayout.getTabAt(Category.LOCALITY.ordinal)?.setCustomView(R.layout.view_spinner_small)
            }
        })

        newObservationViewModel.showPrompt.observe(viewLifecycleOwner, Observer {
            val dialog = PromptFragment()
            dialog.setTargetFragment(this, 10)
            dialog.arguments = Bundle().apply {
                putString(PromptFragment.KEY_TITLE, it.title)
                putString(PromptFragment.KEY_MESSAGE, it.message)
                putString(PromptFragment.KEY_POSITIVE, it.yes)
                putString(PromptFragment.KEY_NEGATIVE, it.no)
            }
            dialog.show(parentFragmentManager, null)
        })

        newObservationViewModel.showNotification.observe(viewLifecycleOwner, Observer {
            val bitmap = when (it) {
                is NewObservationViewModel.Notification.LocationFound, is NewObservationViewModel.Notification.ObservationUploaded -> BitmapFactory.decodeResource(resources, R.drawable.icon_locality_pin).changeColor(R.color.colorPrimary)
                else -> BitmapFactory.decodeResource(
                    resources,
                    R.drawable.icon_elmessageview_failure
                ).changeColor(R.color.colorRed)
            }

            createToast(it.title, it.message, bitmap)
        })
}

    private fun uploadButtonPressed() {
        val result = newObservationViewModel.prepareForUpload(Session.user.value?.id ?: 0, args.type == Type.Edit)

        result.onError {
            when (it) {
                is NewObservationViewModel.Error.NoLocationDataError -> {
                    viewPager.currentItem = Category.LOCALITY.ordinal
                }

                is NewObservationViewModel.Error.NoSubstrateError -> {
                    viewPager.currentItem = Category.DETAILS.ordinal
                }

                is NewObservationViewModel.Error.NoVegetationTypeError -> {
                    viewPager.currentItem = Category.DETAILS.ordinal
                }

                is NewObservationViewModel.Error.NoMushroomError -> {
                    viewPager.currentItem = Category.SPECIES.ordinal
                }
            }

            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_elmessageview_failure)
            createToast(it.title, it.message, bitmap.changeColor(ResourcesCompat.getColor(resources, R.color.colorRed, null)))
        }


        result.onSuccess {
            if (args.type == Type.Edit) {
                Session.editObservation(args.id.toInt(), it.first, it.second)
            } else {
                Session.uploadObservation(it.first, it.second)
            }
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
        viewPager.currentItem = Category.SPECIES.ordinal
        if (localityIdShown == LOCALITY_ID_FOR_ERROR) localityIdShown = DEFAULT_LOCALITY_ID
        newObservationViewModel.setupAsNew()
    }

    override fun onDestroyView() {
        locationService.setListener(null)
        super.onDestroyView()
    }
}