package com.noque.svampeatlas.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.ExifInterface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.common.util.concurrent.ListenableFuture
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.ResultsAdapter
import com.noque.svampeatlas.extensions.openSettings
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.PredictionResult
import com.noque.svampeatlas.models.RecoveryAction
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.FileManager
import com.noque.svampeatlas.utilities.DeviceOrientation
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.view_models.CameraViewModel
import com.noque.svampeatlas.view_models.NewObservationViewModel
import com.noque.svampeatlas.view_models.factories.CameraViewModelFactory
import com.noque.svampeatlas.views.*
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File


class CameraFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback, PromptFragment.Listener {

    companion object {
        private const val TAG = "CameraFragment"
        private const val CODE_PERMISSION = 200
        private const val CODE_LIBRARY_REQUEST = 1234
    }

    enum class Type {
        NEW_OBSERVATION,
        IMAGE_CAPTURE,
        IDENTIFY
    }

    sealed class Error(title: String, message: String, recoveryAction: RecoveryAction): AppError(
        title,
        message,
        recoveryAction
    ) {
        class PermissionsError(resources: Resources): Error(
            resources.getString(R.string.error_camera_permissionsError_title), resources.getString(
                R.string.error_camera_permissionsError_message
            ), RecoveryAction.OPENSETTINGS
        )
        class CaptureError(resources: Resources): Error(
            resources.getString(R.string.error_camera_cameraError_title), resources.getString(
                R.string.error_camera_unknown_message
            ), RecoveryAction.TRYAGAIN
        ) }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "permissions results ${view} ${cameraControl}")
        if (requestCode == CODE_PERMISSION && view != null) {
            cameraView.post {
                startSessionIfNeeded()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    // Objects
    private val args: CameraFragmentArgs by navArgs()
    private var cameraControl: CameraControl? = null
    private var photoFile: File? = null

    private var previewUseCase: Preview? = null
    private var imageCaptureUseCase: ImageCapture? = null

    private val locationManager: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private val deviceOrientation by lazy { DeviceOrientation() }
    private var currentOrientation = Surface.ROTATION_0
    private var sensorManager: SensorManager? = null

    // Views
    private var toolbar by autoCleared<Toolbar>()
    private var container by autoCleared<MotionLayout>()
    private var cameraView by autoCleared<PreviewView>()
    private var resultsView by autoCleared<ResultsView>()
    private var cameraControlsView by autoCleared<CameraControlsView>()
    private var backgroundView by autoCleared<BackgroundView>()
    private var imageView by autoCleared<ImageView>()
    private var zoomControlsView by autoCleared<ZoomControlsView>()


    // View models

    private val cameraViewModel by lazy { ViewModelProvider(
        this, CameraViewModelFactory(
            args.type,
            requireActivity().application
        )
    ).get(CameraViewModel::class.java) }

    private val newObservationViewModel by lazy { ViewModelProvider(requireActivity()).get(
        NewObservationViewModel::class.java
    ) }

    // Listeners
    override fun positiveButtonPressed() {
        SharedPreferences.setSaveImages(true)
        photoFile?.let {
            GlobalScope.launch {
                FileManager.saveTempImage(it, FileManager.createFile(requireContext()), requireContext())
            }
        }
    }

    override fun negativeButtonPressed() {
        SharedPreferences.setSaveImages(false)
    }

    private val resultsAdapterListener by lazy {
        object : ResultsAdapter.Listener {
            override fun reloadSelected() {  cameraViewModel.reset(); }
            override fun predictionResultSelected(predictionResult: PredictionResult) {
                val state = cameraViewModel.predictionResultsState.value
                val action = CameraFragmentDirections.actionGlobalMushroomDetailsFragment(
                    predictionResult.mushroom.id,
                    DetailsFragment.TakesSelection.SELECT,
                    DetailsFragment.Type.SPECIES,
                    (cameraViewModel.imageFileState.value as? State.Items)?.items?.absolutePath,
                    if (state is State.Items) PredictionResult.getNotes(
                        predictionResult,
                        state.items
                    ) else null
                )

                findNavController().navigate(action)
            }
        }
    }

   private val sensorEventListener by lazy {
       object : SensorEventListener {
           override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
           override fun onSensorChanged(p0: SensorEvent?) {
               val surfaceRotation: Int
               when (deviceOrientation.orientation) {
                   ExifInterface.ORIENTATION_ROTATE_90 -> surfaceRotation = Surface.ROTATION_0
                   ExifInterface.ORIENTATION_NORMAL -> surfaceRotation = Surface.ROTATION_90
                   ExifInterface.ORIENTATION_ROTATE_270 -> surfaceRotation = Surface.ROTATION_180
                   ExifInterface.ORIENTATION_ROTATE_180 -> surfaceRotation = Surface.ROTATION_270
                   else -> surfaceRotation = Surface.ROTATION_0
               }

               if (currentOrientation != surfaceRotation) {
                   currentOrientation = surfaceRotation
                   imageCaptureUseCase?.targetRotation = surfaceRotation
                   rotateViews(surfaceRotation)
               }
           }
       }
   }

    private val onExitButtonPressed = View.OnClickListener { findNavController().navigateUp() }

    private val cameraControlsViewListener by lazy {
        object: CameraControlsView.Listener {
            override fun captureButtonPressed() {
                val metadata = ImageCapture.Metadata()

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager.lastLocation.addOnCompleteListener {
                        metadata.location = it.result
                        takePicture(metadata)
                    }
                } else {
                    takePicture(metadata)
                }
            }

            override fun resetButtonPressed() {
                cameraViewModel.reset()
            }

            override fun photoLibraryButtonPressed() {
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "image/*"
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
                    startActivityForResult(intent, CODE_LIBRARY_REQUEST)
            }

            override fun actionButtonPressed(state: CameraControlsView.State) {
                when (state) {
                    CameraControlsView.State.CONFIRM -> {
                        val imageFileState = cameraViewModel.imageFileState.value
                        if (imageFileState is State.Items) newObservationViewModel.appendImage(imageFileState.items)
                        findNavController().navigateUp()
                    }
                    CameraControlsView.State.CAPTURE_NEW -> findNavController().navigateUp()
                    else -> return
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val cameraViewTouchListener = View.OnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                val point = cameraView.meteringPointFactory.createPoint(motionEvent.x, motionEvent.y)
                val action = FocusMeteringAction.Builder(point).build()
                cameraControl?.startFocusAndMetering(action)
                true
            } else {
                false
            }
        }

    private val zoomControlsViewListener = object: ZoomControlsView.Listener {
        override fun zoomLevelSet(zoomRatio: Float) {
            cameraControl?.setLinearZoom(zoomRatio)
        }
        override fun collapsed() {}
        override fun expanded() {}
    }

    private fun takePicture(metadata: ImageCapture.Metadata) {
        photoFile = FileManager.createTempFile(requireContext()).also {
            cameraControlsView.configureState(CameraControlsView.State.LOADING)
            imageCaptureUseCase?.takePicture(
                ImageCapture.OutputFileOptions.Builder(it).setMetadata(metadata).build(),
                ContextCompat.getMainExecutor(requireContext()),
                onImageSavedCallback
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                container.postDelayed(
                    {
                        container.foreground = ColorDrawable(Color.WHITE)
                        container.postDelayed({ container.foreground = null }, 150)
                    }, 400
                )
            }
        }
    }

    private fun handleImageSaving(photoFile: File) {
        val saveImages = SharedPreferences.getSaveImages()
        if (saveImages == null) {
            PromptFragment().also {
                it.arguments = Bundle().also { bundle ->
                    bundle.putString(PromptFragment.KEY_TITLE, getString(R.string.prompt_shouldSaveImages_title))
                    bundle.putString(PromptFragment.KEY_MESSAGE, getString(R.string.prompt_shouldSaveImages_message))
                    bundle.putString(PromptFragment.KEY_POSITIVE, getString(R.string.prompt_shouldSaveImages_positive))
                    bundle.putString(PromptFragment.KEY_NEGATIVE, getString(R.string.prompt_shouldSaveImages_negative))
                }
                it.setTargetFragment(this@CameraFragment, 0)
                it.show(parentFragmentManager, null)
            }
        } else {
            GlobalScope.launch {
                if (saveImages) FileManager.saveTempImage(photoFile, FileManager.createFile(requireContext()), requireContext())
            }
        }
    }

    private val onImageSavedCallback by lazy {
        object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val photoFile = photoFile
                if (photoFile != null) {
                    cameraViewModel.setImageFile(photoFile)
                    handleImageSaving(photoFile)
                } else cameraViewModel.setImageFileError(Error.CaptureError(resources))
            }
            override fun onError(exception: ImageCaptureException) = cameraViewModel.setImageFileError(Error.CaptureError(resources))
        }
    }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == CODE_LIBRARY_REQUEST && data != null) {
                data.data?.let {
                    cameraViewModel.setImageFile(it, FileManager.createTempFile(requireContext()))
                }
            }
            super.onActivityResult(requestCode, resultCode, data)
        }

        @SuppressLint("SourceLockedOrientationActivity")
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            setHasOptionsMenu(true)
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            return inflater.inflate(R.layout.fragment_camera, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            this.sensorManager = (requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager).also {
                it.registerListener(
                    sensorEventListener, it.getDefaultSensor(
                        Sensor.TYPE_ROTATION_VECTOR
                    ), SensorManager.SENSOR_DELAY_UI
                )
                it.registerListener(
                    deviceOrientation.eventListener, it.getDefaultSensor(
                        Sensor.TYPE_ACCELEROMETER
                    ), SensorManager.SENSOR_DELAY_UI
                )
                it.registerListener(
                    deviceOrientation.eventListener, it.getDefaultSensor(
                        Sensor.TYPE_MAGNETIC_FIELD
                    ), SensorManager.SENSOR_DELAY_UI
                )
            }

            initViews()
            setupViews()
            setupViewModels()
        }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_camera_fragment, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_cameraFragment_aboutButton -> {
                val bundle = Bundle()
                bundle.putSerializable(TermsFragment.KEY_TYPE, TermsFragment.Type.CAMERAHELPER)

                val dialog = TermsFragment()
                dialog.arguments = bundle
                dialog.show(childFragmentManager, null)
            }
        }
        return super.onOptionsItemSelected(item)
    }

        override fun onResume() {
            super.onResume()
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            (requireActivity() as BlankActivity).hideSystemBars()
            validateState()
        }

        override fun onStart() {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            (requireActivity() as BlankActivity).hideSystemBars()
            super.onStart()
        }

        override fun onStop() {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            (requireActivity() as BlankActivity).showSystemBars()
            super.onStop()
        }

        private fun initViews() {
            zoomControlsView = cameraFragment_zoomControlsView
            toolbar = cameraFragment_toolbar
            container = cameraFragment_root
            imageView = cameraFragment_imageView
            cameraView = cameraFragment_cameraView
            cameraControlsView = cameraFragment_cameraControlsView
            backgroundView = cameraFragment_backgroundView
            resultsView = cameraFragment_resultsView
        }

        @SuppressLint("ClickableViewAccessibility")
        private fun setupViews() {
            when (args.type) {
                Type.IMAGE_CAPTURE -> {
                    toolbar.setNavigationIcon(R.drawable.glyph_cancel)
                    toolbar.setNavigationOnClickListener(onExitButtonPressed)
                }
                Type.IDENTIFY -> {
                    (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
                    cameraControlsView.configureState(CameraControlsView.State.CAPTURE)
                }
                Type.NEW_OBSERVATION -> {
                    (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
                    cameraControlsView.configureState(CameraControlsView.State.CAPTURE_NEW)
                }
            }

            cameraControlsView.setListener(cameraControlsViewListener)
            zoomControlsView.setListener(zoomControlsViewListener)
            resultsView.setListener(resultsAdapterListener)
            cameraView.setOnTouchListener(cameraViewTouchListener)
        }


        private fun setupViewModels() {
            cameraViewModel.imageFileState.observe(
                viewLifecycleOwner, Observer { state ->
                    when (state) {
                        is State.Items -> setImageState(state.items)
                        is State.Error -> setError(state.error)
                        is State.Empty -> reset()
                    }
                })

            cameraViewModel.predictionResultsState.observe(viewLifecycleOwner, Observer { state ->
                when (state) {
                    is State.Loading -> { cameraControlsView.configureState(CameraControlsView.State.LOADING) }
                    is State.Error -> {
                        container.transitionToEnd()
                        resultsView.showError(state.error)
                    }
                    is State.Items -> {
                        container.transitionToEnd()
                        resultsView.showResults(state.items)
                    }

                    is State.Empty -> {
                        resultsView.reset()
                        container.transitionToStart()
                    }
                }
            })
        }

        private fun validateState() {
            if (args.type == Type.IDENTIFY && !SharedPreferences.hasAcceptedIdentificationTerms()) {
                TermsFragment().also {
                    it.arguments = Bundle().also { bundle -> bundle.putSerializable(TermsFragment.KEY_TYPE, TermsFragment.Type.IDENTIFICATION) }
                    it.listener = object: TermsFragment.Listener {
                        override fun onDismiss(termsAccepted: Boolean) = requestPermissions(arrayOf(Manifest.permission.CAMERA), CODE_PERMISSION)
                    }
                    it.show(childFragmentManager, null)
                }
            } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraView.post {
                    startSessionIfNeeded()
                }
            } else {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), CODE_PERMISSION)
            }
        }

        private fun reset() {
            backgroundView.reset()
            imageView.setImageResource(android.R.color.transparent)
            imageView.setBackgroundResource(android.R.color.transparent)
            when (args.type) {
                Type.IMAGE_CAPTURE, Type.IDENTIFY -> cameraControlsView.configureState(CameraControlsView.State.CAPTURE)
                Type.NEW_OBSERVATION -> cameraControlsView.configureState(CameraControlsView.State.CAPTURE_NEW)
            }
        }

        private fun startSessionIfNeeded() {
            Log.d(TAG, "StartSession if needed")
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
                cameraProviderFuture.addListener(Runnable {
                    val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                    previewUseCase =
                        Preview.Builder()
                            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                            .setTargetRotation(Surface.ROTATION_0)

                            .build()
                    imageCaptureUseCase = ImageCapture.Builder().build()
                    cameraProvider.unbindAll()
                    try {

                            val camera = cameraProvider.bindToLifecycle(
                                this,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                previewUseCase,
                                imageCaptureUseCase
                            )
                            previewUseCase?.setSurfaceProvider(cameraView.surfaceProvider)
                            cameraControl = camera.cameraControl
                        } catch (error: Exception) {
                            cameraViewModel.setImageFileError(Error.CaptureError(resources))
                        }

                },  ContextCompat.getMainExecutor(requireContext()))
            }
        }

        private fun setImageState(file: File) {
            backgroundView.reset()
            imageView.setBackgroundResource(android.R.color.black)
            imageView.apply {
                Glide.with(this)
                    .load(file)
                    .into(this)
            }

            if (args.type == Type.IMAGE_CAPTURE || args.type == Type.NEW_OBSERVATION) cameraControlsView.configureState(CameraControlsView.State.CONFIRM)
        }

        private fun setError(error: AppError) {
            backgroundView.setErrorWithHandler(error, error.recoveryAction) {
                if (error.recoveryAction == RecoveryAction.OPENSETTINGS) openSettings()
                else if (error.recoveryAction == RecoveryAction.TRYAGAIN) cameraViewModel.reset()
            }
        }

        private fun rotateViews(rotation: Int) {
            val transform = when (rotation) {
                Surface.ROTATION_0 -> 0F
                Surface.ROTATION_90 -> 90F
                Surface.ROTATION_180 -> 180F
                Surface.ROTATION_270 -> -90F
                else -> 0F
            }
                cameraControlsView.rotate(transform, 350)
                zoomControlsView.rotate(transform, 350)
        }

        override fun onDestroyView() {
            sensorManager?.unregisterListener(deviceOrientation.eventListener)
            sensorManager?.unregisterListener(sensorEventListener)
            previewUseCase?.setSurfaceProvider(null)
            super.onDestroyView()
        }
}


