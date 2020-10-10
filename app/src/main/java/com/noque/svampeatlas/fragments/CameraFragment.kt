package com.noque.svampeatlas.fragments

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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.transition.TransitionManager
import android.util.Log
import android.util.Rational
import android.view.*
import android.widget.ImageView
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
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
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.ResultsAdapter
import com.noque.svampeatlas.extensions.openSettings
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.PredictionResult
import com.noque.svampeatlas.models.RecoveryAction
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.FileManager
import com.noque.svampeatlas.utilities.DeviceOrientation
import com.noque.svampeatlas.utilities.SharedPreferencesHelper
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.view_models.CameraViewModel
import com.noque.svampeatlas.view_models.NewObservationViewModel
import com.noque.svampeatlas.view_models.factories.CameraViewModelFactory
import com.noque.svampeatlas.views.*
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors


class CameraFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {

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

    // Objects
    private val args: CameraFragmentArgs by navArgs()
    private var cameraControl: CameraControl? = null
    private var imageCapture: ImageCapture? = null
    private var photoFile: File? = null

    private val locationManager: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private val deviceOrientation by lazy { DeviceOrientation() }
    private val rootConstraintSet by lazy { ConstraintSet() }
    private val resultsConstraintSet by lazy { ConstraintSet() }

    private var currentOrientation = Surface.ROTATION_0
    private var sensorManager: SensorManager? = null

    // Views
    private var toolbar by autoCleared<Toolbar>()
    private var container by autoCleared<ConstraintLayout>()
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

    private val newObservationViewModel by lazy { ViewModelProvider(this).get(
        NewObservationViewModel::class.java
    ) }


    // Listeners
    private val resultsAdapterListener by lazy {
        object : ResultsAdapter.Listener {
            override fun reloadSelected() { cameraViewModel.reset() }

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
                   ExifInterface.ORIENTATION_ROTATE_90 -> {
                       surfaceRotation = Surface.ROTATION_0
                   }
                   ExifInterface.ORIENTATION_NORMAL -> {
                       surfaceRotation = Surface.ROTATION_90
                   }
                   ExifInterface.ORIENTATION_ROTATE_270 -> {
                       surfaceRotation = Surface.ROTATION_180
                   }
                   ExifInterface.ORIENTATION_ROTATE_180 -> {
                       surfaceRotation = Surface.ROTATION_270
                   }
                   else -> {
                       surfaceRotation = Surface.ROTATION_0
                   }
               }

               if (currentOrientation != surfaceRotation) {
                   currentOrientation = surfaceRotation
                   rotateViews(surfaceRotation)
               }
           }
       }
   }

    private val onExitButtonPressed = View.OnClickListener {
        findNavController().navigateUp()
    }

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
                    CameraControlsView.State.CAPTURE_NEW -> {
                        findNavController().navigateUp()
                    }
                    else -> return
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val cameraViewTouchListener = View.OnTouchListener { view, motionEvent ->
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
        photoFile = FileManager.createTempFile(requireContext())

        val imageCapture = imageCapture ?: return
        imageCapture.targetRotation = currentOrientation
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(photoFile!!).setMetadata(metadata).build(),
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

    private val onImageSavedCallback by lazy {
        object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                cameraViewModel.setImageFile(photoFile!!)
                cameraViewModel.saveImage(FileManager.createFile(requireContext()))
            }

            override fun onError(exception: ImageCaptureException) {
                cameraViewModel.setImageFileError(Error.CaptureError(resources))
            }
        }
    }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            if (requestCode == CODE_PERMISSION) {
                if ((grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED)) {
                    cameraViewModel.start()
                } else {
                    cameraViewModel.setImageFileError(Error.PermissionsError(resources))
                }
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
           Log.d(TAG, "On activity result")

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
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            (requireActivity() as BlankActivity).hideSystemBars()
            super.onResume()
        }

        override fun onStart() {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            (requireActivity() as BlankActivity).hideSystemBars()
            validateState()
            super.onStart()
        }

        override fun onStop() {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            (requireActivity() as BlankActivity).showSystemBars()
            super.onStop()
        }

        private fun initViews() {
            rootConstraintSet.clone(cameraFragment_root)
            resultsConstraintSet.clone(requireContext(), R.layout.fragment_camera_results)

            zoomControlsView = cameraFragment_zoomControlsView
            toolbar = cameraFragment_toolbar
            container = cameraFragment_root
            imageView = cameraFragment_imageView
            cameraView = cameraFragment_cameraView
            cameraControlsView = cameraFragment_cameraControlsView
            backgroundView = cameraFragment_backgroundView
//            resultsView = cameraFragment_resultsView
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
//                    resultsView?.setListener(resultsAdapterListener)
                }
                Type.NEW_OBSERVATION -> {
                    (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
                    cameraControlsView.configureState(CameraControlsView.State.CAPTURE_NEW)
                }
            }

            cameraControlsView.setListener(cameraControlsViewListener)
            zoomControlsView.setListener(zoomControlsViewListener)
            cameraView.setOnTouchListener(cameraViewTouchListener)
        }


        private fun setupViewModels() {
            cameraViewModel.imageFileState.observe(
                viewLifecycleOwner, Observer { state ->
                    when (state) {
                        is State.Items -> {
                            setImageState(state.items)
                        }
                        is State.Error -> {
                            setError(state.error)
                        }
                        is State.Empty -> {
                            startSession()
                        }
                    }
                })

//            cameraViewModel.predictionResultsState.observe(viewLifecycleOwner, androidx.lifecycle.Observer { state ->
//                captureButtonSpinner?.visibility = View.GONE
//
//                when (state) {
//                    is State.Loading -> { captureButtonSpinner?.visibility = View.VISIBLE }
//                    is State.Error -> {
//                        expand()
//                        captureButton?.visibility = View.GONE
//                        resultsView?.visibility = View.VISIBLE
//                        resultsView?.showError(state.error)
//                    }
//                    is State.Items -> {
//                        expand()
//                        captureButton?.visibility = View.GONE
//                        resultsView?.visibility = View.VISIBLE
//                        resultsView?.showResults(state.items)
//                    }
//
//                    is State.Empty -> {
//                        collapse()
//                    }
//                }
//            })
        }

        private fun validateState() {
            if (args.type == Type.IDENTIFY && !SharedPreferencesHelper(requireContext()).hasAcceptedIdentificationTerms()) {
                val bundle = Bundle()
                bundle.putSerializable(TermsFragment.KEY_TYPE, TermsFragment.Type.IDENTIFICATION)
                val dialog = TermsFragment()
                dialog.arguments = bundle
                dialog.listener = object: TermsFragment.Listener {
                    override fun onDismiss(termsAccepted: Boolean) {
                        requestPermissions(
                            arrayOf(android.Manifest.permission.CAMERA),
                            CODE_PERMISSION
                        )
                    }
                }
                dialog.show(childFragmentManager, null)

            } else if (cameraViewModel.imageFileState.value !is State.Items && cameraViewModel.imageFileState.value !is State.Loading) {
                Log.d(TAG, "State validated, asking for permissions anew")
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CODE_PERMISSION)
            }
        }

        private fun stopCamera() {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener(Runnable {
                cameraProviderFuture.get().unbindAll()
            }, ContextCompat.getMainExecutor(requireContext()))
        }

        @SuppressLint("MissingPermission")
        private fun startSession() {
            backgroundView.reset()
            imageView.setImageResource(android.R.color.transparent)


            val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
            cameraProviderFuture.addListener(Runnable {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setTargetRotation(cameraView.display.rotation)
                    .build().also { it.setSurfaceProvider(cameraView.surfaceProvider) }

                val imageCapture = ImageCapture.Builder()
                    .build().also { this.imageCapture = it }

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        viewLifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )

                    cameraControl = camera.cameraControl
                } catch (error: Exception) {
                }
            },  ContextCompat.getMainExecutor(requireContext()))
        }

        private fun setImageState(file: File) {
            stopCamera()
            backgroundView.reset()
            imageView.apply {
                Glide.with(this)
                    .load(file)
                    .into(this)
            }

            when (args.type) {
                Type.IMAGE_CAPTURE, Type.NEW_OBSERVATION -> {
                    cameraControlsView.configureState(CameraControlsView.State.CONFIRM)
                }

                Type.IDENTIFY -> {
                    cameraControlsView.configureState(CameraControlsView.State.HIDDEN)
                }
            }
        }

        private fun setError(error: AppError) {
            backgroundView.setErrorWithHandler(error, error.recoveryAction) {
                if (error.recoveryAction == RecoveryAction.OPENSETTINGS) {
                    openSettings()
                }
                else if (error.recoveryAction == RecoveryAction.TRYAGAIN) {
                    cameraViewModel.reset()
                }
            }
        }


        private fun expand() {
            TransitionManager.beginDelayedTransition(cameraFragment_root)
            val constraint =  resultsConstraintSet
            constraint.applyTo(cameraFragment_root)
        }

        private fun collapse() {
            TransitionManager.beginDelayedTransition(cameraFragment_root)
            val constraint =  rootConstraintSet
            constraint.applyTo(cameraFragment_root)
        }

        private fun rotateViews(rotation: Int) {
            val transform = when (rotation) {
                Surface.ROTATION_0 -> {
                    0F
                }
                Surface.ROTATION_90 -> {
                    90F
                }
                Surface.ROTATION_180 -> {
                    180F
                }
                Surface.ROTATION_270 -> {
                    -90F
                }
                else -> {
                    0F
                }
            }
                cameraControlsView.rotate(transform, 350)
                zoomControlsView.rotate(transform, 350)
        }

        override fun onDestroyView() {
            sensorManager?.unregisterListener(deviceOrientation.eventListener)
            sensorManager?.unregisterListener(sensorEventListener)
            super.onDestroyView()
        }
    }


