package com.noque.svampeatlas.fragments

import android.content.Context
import android.content.DialogInterface
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
import android.provider.Settings
import android.transition.TransitionManager
import android.util.Log
import android.util.Rational
import android.view.*
import android.widget.*
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import com.noque.svampeatlas.models.AppError

import com.noque.svampeatlas.R
import com.noque.svampeatlas.views.BackgroundView
import com.noque.svampeatlas.view_models.NewObservationViewModel
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.noque.svampeatlas.BuildConfig
import com.noque.svampeatlas.adapters.ResultsAdapter
import com.noque.svampeatlas.models.PredictionResult
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.utilities.AutoFitPreviewBuilder
import com.noque.svampeatlas.utilities.DeviceOrientation
import com.noque.svampeatlas.utilities.SharedPreferencesHelper
import com.noque.svampeatlas.view_models.CameraViewModel
import com.noque.svampeatlas.view_models.factories.CameraViewModelFactory
import com.noque.svampeatlas.views.BlankActivity
import com.noque.svampeatlas.views.ResultsView
import kotlinx.android.synthetic.main.fragment_camera.cameraFragment_backgroundView
import kotlinx.android.synthetic.main.fragment_camera.cameraFragment_captureButton
import kotlinx.android.synthetic.main.fragment_camera.cameraFragment_imageView
import kotlinx.android.synthetic.main.fragment_camera.cameraFragment_libraryButton
import kotlinx.android.synthetic.main.fragment_camera.cameraFragment_noPhotoButton
import kotlinx.android.synthetic.main.fragment_camera.cameraFragment_root
import kotlinx.android.synthetic.main.fragment_camera.cameraFragment_textureView
import kotlinx.android.synthetic.main.fragment_camera.cameraFragment_toolbar
import java.text.SimpleDateFormat
import java.util.*


class CameraFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        val TAG = "CameraFragment"
        val CODE_PERMISSION = 200
        val CODE_LIBRARYREQUEST = 1234
    }

    enum class Type {
        NEWOBSERVATION,
        IMAGECAPTURE,
        IDENTIFY
    }

    sealed class Error(title: String, message: String): AppError(title, message) {
        class PermissionsError(resources: Resources): Error(resources.getString(R.string.cameraFragment_permissionsError_title), resources.getString(R.string.cameraFragment_permissionsError_message))
        class CaptureError(resources: Resources): Error(resources.getString(R.string.cameraFragment_captureError_title), resources.getString(R.string.cameraFragment_captureError_message)) }

    // Objects

    private val args: CameraFragmentArgs by navArgs()

    private val deviceOrientation by lazy { DeviceOrientation() }
    private val rootConstraintSet by lazy { ConstraintSet() }
    private val resultsConstraintSet by lazy { ConstraintSet() }

    private var currentOrientation = Surface.ROTATION_0
    private var sensorManager: SensorManager? = null

    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null

    // Views
    private var toolbar: Toolbar? = null
    private var container: ConstraintLayout? = null
    private var textureView: TextureView? = null
    private var resultsView: ResultsView? = null
    private var captureButton: ImageButton? = null
    private var captureButtonSpinner: ProgressBar? = null
    private var backgroundView: BackgroundView? = null
    private var photoLibraryButton: ImageButton? = null
    private var actionButton: Button? = null
    private var imageView: ImageView? = null


    // View models

    private val cameraViewModel by lazy { ViewModelProviders.of(this, CameraViewModelFactory(args.type, requireActivity().application)). get(CameraViewModel::class.java) }
    private val newObservationViewModel by lazy { ViewModelProviders.of(requireActivity()).get(NewObservationViewModel::class.java) }


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
                    if (state is State.Items) PredictionResult.getNotes(predictionResult, state.items) else null
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
               val aspectRatio: Rational

               when (deviceOrientation.orientation) {
                   ExifInterface.ORIENTATION_ROTATE_90 -> {
                       aspectRatio = Rational(3, 4)
                       surfaceRotation = Surface.ROTATION_0
                   }
                   ExifInterface.ORIENTATION_NORMAL -> {
                       aspectRatio = Rational(4, 3)
                       surfaceRotation = Surface.ROTATION_90
                   }
                   ExifInterface.ORIENTATION_ROTATE_270 -> {
                       aspectRatio = Rational(3, 4)
                       surfaceRotation = Surface.ROTATION_180
                   }
                   ExifInterface.ORIENTATION_ROTATE_180 -> {
                       aspectRatio = Rational(4, 3)
                       surfaceRotation = Surface.ROTATION_270
                   }
                   else -> {
                       aspectRatio = Rational(3, 4)
                       surfaceRotation = Surface.ROTATION_0
                   }
               }

               if (currentOrientation != surfaceRotation) {
                   currentOrientation = surfaceRotation
                   rotateViews(surfaceRotation)
                   imageCapture?.setTargetRotation(surfaceRotation)
                   imageCapture?.setTargetAspectRatio(aspectRatio)
               }
           }
       }
   }

    private val onExitButtonPressed by lazy {
        View.OnClickListener {
            findNavController().navigateUp()
        }
    }

    private val captureButtonPressed by lazy {
        View.OnClickListener {
            imageCapture?.let {
                it.takePicture(BlankActivity.createTempFile(requireContext()), onImageSavedListener)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    container?.postDelayed(
                        {
                            container?.foreground = ColorDrawable(Color.WHITE)
                            container?.postDelayed({ container?.foreground = null }, 150)
                        }, 400
                    )
                }
            }
        }
    }

    private val actionButtonPressed by lazy {
        View.OnClickListener {
            when (args.type) {
                Type.IMAGECAPTURE -> {
                    cameraViewModel.saveImage(BlankActivity.createFile(requireContext()))
                }
                Type.IDENTIFY -> {}
                Type.NEWOBSERVATION -> {
                    if (cameraViewModel.imageFileState.value is State.Items) {
                        cameraViewModel.saveImage(BlankActivity.createFile(requireContext()))
                    } else {
                        findNavController().navigateUp()
                    }
                }
            }
        }
    }

    private val photoLibraryButtonPressed by lazy {
        View.OnClickListener {
            if (cameraViewModel.imageFileState.value is State.Items) {
                cameraViewModel.reset()
            } else {
                stopCamera()

                val intent = Intent(Intent.ACTION_PICK)
                intent.setType("image/*")
                intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
                startActivityForResult(intent, CODE_LIBRARYREQUEST)
            }
        }
    }

        private val onImageSavedListener by lazy {
            object : ImageCapture.OnImageSavedListener {
                override fun onImageSaved(file: File) {
                    cameraViewModel.setImageFile(file)
                }

                override fun onError(
                    useCaseError: ImageCapture.UseCaseError, message: String, cause: Throwable?
                ) {
                    cameraViewModel.setImageFileError(Error.CaptureError(resources))
                }
            }
        }

        override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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

            if (requestCode == CODE_LIBRARYREQUEST && data != null) {
                data.data?.let {
                    cameraViewModel.setImageFile(it, createTempFile())
                }
            }
            super.onActivityResult(requestCode, resultCode, data)
        }


        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            return inflater.inflate(R.layout.fragment_camera, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(deviceOrientation.eventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI)
            sensorManager.registerListener(deviceOrientation.eventListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_UI)
            this.sensorManager = sensorManager

            initViews()
            setupViews()
            setupViewModels()
        }

        override fun onResume() {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            (requireActivity() as BlankActivity).hideSystemBars()
            super.onResume()
        }

        override fun onStart() {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            (requireActivity() as BlankActivity).hideSystemBars()
            Log.d(TAG, "On start")
            validateState()
            super.onStart()
        }

        override fun onStop() {
            requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            (requireActivity() as BlankActivity).showSystemBars()
            stopCamera()
            super.onStop()
        }

        private fun initViews() {

            rootConstraintSet.clone(cameraFragment_root)
            resultsConstraintSet.clone(requireContext(), R.layout.fragment_camera_results)

            toolbar = cameraFragment_toolbar
            actionButton = cameraFragment_noPhotoButton
            container = cameraFragment_root
            imageView = cameraFragment_imageView
            textureView = cameraFragment_textureView
            resultsView = cameraFragment_resultsView
            captureButton = cameraFragment_captureButton
            captureButtonSpinner = cameraFragment_captureButtonSpinner
            backgroundView = cameraFragment_backgroundView
            photoLibraryButton = cameraFragment_libraryButton
        }

        private fun setupViews() {
            when (args.type) {
                Type.IMAGECAPTURE -> {
                    toolbar?.setNavigationIcon(R.drawable.glyph_cancel)
                    toolbar?.setNavigationOnClickListener(onExitButtonPressed)
                }
                Type.IDENTIFY -> {
                    (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
                    resultsView?.setListener(resultsAdapterListener)
                }
                Type.NEWOBSERVATION -> {
                    (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
                    actionButton?.setText(R.string.cameraFragment_actionButton_noPhoto)
                }
            }

            captureButton?.setOnClickListener(captureButtonPressed)
            photoLibraryButton?.setOnClickListener(photoLibraryButtonPressed)
            actionButton?.setOnClickListener(actionButtonPressed)
        }


        private fun setupViewModels() {
            cameraViewModel.imageSaveState.observe(viewLifecycleOwner, androidx.lifecycle.Observer { state ->
                when (state) {
                    is State.Loading -> { backgroundView?.setLoading() }
                    is State.Items -> {
                        newObservationViewModel.appendImage(state.items)
                        findNavController().navigateUp()
                    }
                }
            })

            cameraViewModel.imageFileState.observe(viewLifecycleOwner, androidx.lifecycle.Observer { state ->
                when (state) {
                    is State.Items -> { setImageState(state.items) }
                    is State.Error -> { setError(state.error) }
                    is State.Empty -> { startSession() }
                }
            })

            cameraViewModel.predictionResultsState.observe(viewLifecycleOwner, androidx.lifecycle.Observer { state ->
                captureButtonSpinner?.visibility = View.GONE

                when (state) {
                    is State.Loading -> { captureButtonSpinner?.visibility = View.VISIBLE }
                    is State.Error -> {
                        expand()
                        captureButton?.visibility = View.GONE
                        resultsView?.visibility = View.VISIBLE
                        resultsView?.showError(state.error)
                    }
                    is State.Items -> {
                        expand()
                        captureButton?.visibility = View.GONE
                        resultsView?.visibility = View.VISIBLE
                        resultsView?.showResults(state.items)
                    }

                    is State.Empty -> {
                        collapse()
                    }
                }
            })
        }

        private fun validateState() {
            if (args.type == Type.IDENTIFY && !SharedPreferencesHelper(requireContext()).hasAcceptedIdentificationTerms()) {
                val bundle = Bundle()
                bundle.putSerializable(TermsFragment.KEY_TYPE, TermsFragment.Type.IDENTIFICATION)

                val dialog = TermsFragment()
                dialog.arguments = bundle
                dialog.listener = object: TermsFragment.Listener {
                    override fun onDismiss(termsAccepted: Boolean) {
                        requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CODE_PERMISSION)
                    }
                }
                dialog.show(childFragmentManager, null)

            } else if (imageCapture == null && preview == null && cameraViewModel.imageFileState.value !is State.Items && cameraViewModel.imageFileState.value !is State.Loading) {
                Log.d(TAG, "State validated, asking for permissions anew")
                requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CODE_PERMISSION)
            }
        }

        private fun startSession() {
            backgroundView?.reset()
            if (CameraX.isBound(imageCapture) && CameraX.isBound(preview)) { return }
            imageView?.setImageDrawable(null)
            imageView?.visibility = View.GONE
            textureView?.visibility = View.VISIBLE
            if (args.type == Type.NEWOBSERVATION) actionButton?.setText(R.string.cameraFragment_actionButton_noPhoto)


            captureButton?.visibility = View.VISIBLE
            photoLibraryButton?.visibility = View.VISIBLE
            photoLibraryButton?.setImageDrawable(resources.getDrawable(R.drawable.icon_photo_library, null))
            resultsView?.visibility = View.GONE

            val viewFinderConfig = PreviewConfig.Builder().apply {
                setLensFacing(CameraX.LensFacing.BACK)
                setTargetRotation(currentOrientation)
                setTargetAspectRatio(Rational(3,4))
            }.build()

            preview = AutoFitPreviewBuilder.build(viewFinderConfig, textureView!!)

            val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
                setLensFacing(CameraX.LensFacing.BACK)
                setTargetAspectRatio(Rational(3, 4))
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                setTargetRotation(currentOrientation)
            }.build()

            imageCapture = ImageCapture(imageCaptureConfig)

            CameraX.bindToLifecycle(viewLifecycleOwner, preview, imageCapture)
        }

        private fun setImageState(file: File) {
            backgroundView?.reset()
            textureView?.visibility = View.INVISIBLE
            stopCamera()

            imageView?.let {
                it.visibility = View.VISIBLE
                Glide.with(it)
                    .load(file)
                    .into(it)
            }


            when (args.type) {
                Type.IMAGECAPTURE, Type.NEWOBSERVATION -> {
                    captureButton?.visibility = View.GONE
                    photoLibraryButton?.visibility = View.VISIBLE
                    actionButton?.visibility = View.VISIBLE
                    photoLibraryButton?.setImageDrawable(resources.getDrawable(R.drawable.icon_back_button, null))
                    actionButton?.text = getText(R.string.cameraFragment_actionButton_usePhoto)
                }


                Type.IDENTIFY -> {
                    photoLibraryButton?.visibility = View.GONE
                    actionButton?.visibility = View.GONE
                }
            }
        }

        private fun setError(error: AppError) {
            when (error) {
                is Error.PermissionsError -> {
                    backgroundView?.setErrorWithHandler(error, resources.getString(R.string.cameraFragment_permissionsError_handler)) {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    }
                }

                else -> {
                    backgroundView?.setErrorWithHandler(error, resources.getString(R.string.cameraFragment_captureError_handler)) {
                        cameraViewModel.reset()
                    }
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
            var transform: Float

            when (rotation) {
                Surface.ROTATION_0 -> { transform = 0F }
                Surface.ROTATION_90 -> { transform = 90F }
                Surface.ROTATION_180 -> { transform = 180F }
                Surface.ROTATION_270 -> { transform =  -90F }
                else -> { transform = 0F }
            }

            photoLibraryButton?.animate()?.rotation(transform)?.setDuration(350)?.start()
            actionButton?.animate()?.rotation(transform)?.setDuration(350)?.start()
        }

        private fun stopCamera() {
            CameraX.unbindAll()
            imageCapture = null
            preview = null
        }

        override fun onDestroy() {
            Log.d(TAG, "On destroy")
            super.onDestroy()
        }

        override fun onDetach() {
            Log.d(TAG, "ON Detach")
            super.onDetach()
        }

        override fun onDestroyView() {
            Log.d(TAG, "On Destroy View")
            toolbar = null
            container = null
            textureView == null
            resultsView == null
            captureButton == null
            captureButtonSpinner == null
            backgroundView == null
            photoLibraryButton == null
            actionButton == null
            imageView == null
            sensorManager?.unregisterListener(deviceOrientation.eventListener)
            sensorManager?.unregisterListener(sensorEventListener)
            super.onDestroyView()
        }
    }


