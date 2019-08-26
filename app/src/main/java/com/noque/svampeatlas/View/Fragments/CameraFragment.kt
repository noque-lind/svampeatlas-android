package com.noque.svampeatlas.View.Fragments

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageButton
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import com.noque.svampeatlas.Model.AppError

import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.BackgroundView
import com.noque.svampeatlas.ViewModel.NewObservationViewModel
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.noque.svampeatlas.BuildConfig
import com.noque.svampeatlas.Utilities.AutoFitPreviewBuilder
import com.noque.svampeatlas.Utilities.ExifUtil
import com.noque.svampeatlas.View.BlankActivity
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class CameraFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        val TAG = "CameraFragment"
        val CODE_PERMISSION = 200
        val CODE_LIBRARYREQUEST = 1234
        val ID_DISPLAY = -1

         val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
         val PHOTO_EXTENSION = ".jpg"
    }

    enum class Type {
        IMAGECAPTURE
    }

    sealed class Error(title: String, message: String): AppError(title, message) {
        class PermissionsError(resources: Resources): Error(resources.getString(R.string.cameraFragment_permissionsError_title), resources.getString(R.string.cameraFragment_permissionsError_message))
        class CaptureError(resources: Resources): Error(resources.getString(R.string.cameraFragment_captureError_title), resources.getString(R.string.cameraFragment_captureError_message))
        }

    // Objects

    private val args: CameraFragmentArgs by navArgs()
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private lateinit var displayManager: DisplayManager
    private lateinit var outputDirectory: File

    private var imageFile: File? = null

    // Views

    private lateinit var exitButton: ImageButton
    private lateinit var container: ConstraintLayout
    private lateinit var viewFinder: TextureView
    private lateinit var captureButton: ImageButton
    private lateinit var backgroundView: BackgroundView
    private lateinit var photoLibraryButton: ImageButton
    private lateinit var textButton: Button
    private lateinit var imageView: ImageView


    // View models

    private lateinit var newObservationViewModel: NewObservationViewModel

    // Listeners

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == ID_DISPLAY) {
                preview?.setTargetRotation(view.display.rotation)
                imageCapture?.setTargetRotation(view.display.rotation)
            }
        } ?: Unit
    }

    private val exitButtonPressed = View.OnClickListener {
        findNavController().navigateUp()
    }

    private val captureButtonPressed = View.OnClickListener {
        val file = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)

        imageCapture?.let {
            val metaData = ImageCapture.Metadata().apply {
                isReversedHorizontal = false
            }

            it.takePicture(file, onImageSavedListener, metaData)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                container.postDelayed(
                    {container.foreground = ColorDrawable(Color.WHITE)
                        container.postDelayed( { container.foreground = null}, 150)
                    }, 400)
            }
        }
    }

    private val photoLibraryButtonPressed = View.OnClickListener {
        if (imageFile != null) {
            imageFile = null
            setImageCaptureState()
        } else {
            stopCamera()

            val intent = Intent(Intent.ACTION_PICK)
            intent.setType("image/*")
            intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png"))
            startActivityForResult(intent, CODE_LIBRARYREQUEST)
        }
    }

    private val onImageSavedListener = object: ImageCapture.OnImageSavedListener {
        override fun onImageSaved(file: File) {
            stopCamera()
            imageFile = file
            setImageCapturedState(file)
        }

        override fun onError(
            useCaseError: ImageCapture.UseCaseError,
            message: String,
            cause: Throwable?
        ) {
            stopCamera()
            backgroundView.setErrorWithHandler(Error.CaptureError(resources), resources.getString(R.string.cameraFragment_captureError_handler)) {
                startCamera()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CODE_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED)) {
                Log.d(TAG, "Permission has been granted")
                viewFinder.post { setImageCaptureState() }
            } else {
                backgroundView.setErrorWithHandler(Error.PermissionsError(resources), resources.getString(R.string.cameraFragment_permissionsError_handler)) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode ==CODE_LIBRARYREQUEST && data != null) {
            val imageUri = data?.data
            val bitmap = MediaStore.Images.Media.getBitmap(context!!.contentResolver, imageUri)

            when (args.type) {
                Type.IMAGECAPTURE -> {
                newObservationViewModel.appendImage(bitmap)
                findNavController().navigateUp()
            }
            }
        } else {
            setImageCaptureState()
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onStart() {
        super.onStart()
        (requireActivity() as BlankActivity).hideSystemBars()
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as BlankActivity).showSystemBars()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
        setupViewModels()

        displayManager = viewFinder.context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        outputDirectory = BlankActivity.getOutputDirectory(requireContext())

        val imageFile = imageFile
        if (imageFile != null) {
            setImageCapturedState(imageFile)
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CODE_PERMISSION)
        }
    }

    private fun initViews() {
        exitButton = cameraFragment_exitButton
        textButton = cameraFragment_noPhotoButton
        container = view as ConstraintLayout
        imageView = cameraFragment_imageView
        viewFinder = cameraFragment_textureView
        captureButton = cameraFragment_captureButton
        backgroundView = cameraFragment_backgroundView
        photoLibraryButton = cameraFragment_libraryButton
    }

    private fun setupViews() {
        viewFinder.post {
            viewFinder.id = ID_DISPLAY
            }

        captureButton.setOnClickListener(captureButtonPressed)
        exitButton.setOnClickListener(exitButtonPressed)
        photoLibraryButton.setOnClickListener(photoLibraryButtonPressed)
    }

    private fun setupViewModels() {
        activity?.let {
            when (args.type) {
                Type.IMAGECAPTURE -> { newObservationViewModel = ViewModelProviders.of(it).get(NewObservationViewModel::class.java) }
            }
        }
    }

    private fun setImageCapturedState(imageFile: File) {
        photoLibraryButton.setImageDrawable(resources.getDrawable(R.drawable.icon_back_button, null))
        captureButton.visibility = View.GONE

        imageView.visibility = View.VISIBLE
        Glide.with(imageView)
            .load(imageFile)
            .into(imageView)

        when (args.type) {
            Type.IMAGECAPTURE -> {
                imageView.visibility = View.VISIBLE
                textButton.setText("Brug billede")
                textButton.setOnClickListener {
                    val imagePath = imageFile.absolutePath
                    val bitmap = BitmapFactory.decodeFile(imagePath)
                    val orientedBitmap = ExifUtil.rotateBitmap(imagePath, bitmap)

                    newObservationViewModel.appendImage(orientedBitmap)
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun setImageCaptureState() {
        startCamera()

        imageView.setImageBitmap(null)
        imageView.visibility = View.GONE
        captureButton.visibility = View.VISIBLE
        photoLibraryButton.setImageDrawable(resources.getDrawable(R.drawable.icon_photo_library, null))
        textButton.text = null
    }

    private fun startCamera() {

        val metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        val viewFinderConfig = PreviewConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        preview = AutoFitPreviewBuilder.build(viewFinderConfig, viewFinder)


        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(CameraX.LensFacing.BACK)
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(viewFinder.display.rotation)
        }.build()

        imageCapture = ImageCapture(imageCaptureConfig)

        CameraX.bindToLifecycle(viewLifecycleOwner, preview, imageCapture)
    }

    private fun stopCamera() {
        CameraX.unbindAll()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        displayManager.unregisterDisplayListener(displayListener)
    }

    private fun createFile(baseFolder: File, format: String, extension: String) =
        File.createTempFile(SimpleDateFormat(format, Locale.getDefault()).format(System.currentTimeMillis()), extension)

//        File(baseFolder, SimpleDateFormat(format, Locale.getDefault())
//            .format(System.currentTimeMillis()) + extension)
}


