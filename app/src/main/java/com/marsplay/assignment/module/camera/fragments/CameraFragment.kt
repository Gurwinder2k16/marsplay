package com.marsplay.assignment.module.camera.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.webkit.MimeTypeMap
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.Metadata
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.Navigation
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.marsplay.assignment.R
import com.marsplay.assignment.constants.Constants
import com.marsplay.assignment.constants.Constants.Companion.FILENAME
import com.marsplay.assignment.constants.Constants.Companion.PHOTO_EXTENSION
import com.marsplay.assignment.constants.Constants.Companion.RATIO_16_9_VALUE
import com.marsplay.assignment.constants.Constants.Companion.RATIO_4_3_VALUE
import com.marsplay.assignment.constants.Constants.Companion.createFile
import com.marsplay.assignment.module.camera.KEY_EVENT_ACTION
import com.marsplay.assignment.module.camera.KEY_EVENT_EXTRA
import com.marsplay.assignment.module.camera.utils.ANIMATION_FAST_MILLIS
import com.marsplay.assignment.module.camera.utils.ANIMATION_SLOW_MILLIS
import com.marsplay.assignment.module.camera.utils.simulateClick
import kotlinx.android.synthetic.main.camera_ui_container.*
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias LumaListener = (luma: Double) -> Unit

class CameraFragment : Fragment() {

    private lateinit var mContainer: ConstraintLayout
    private lateinit var mOutputDirectory: File
    private lateinit var mLocalBroadcastManager: LocalBroadcastManager

    private var mDisplayId: Int = -1
    private var mDefaultLen: Int = CameraSelector.LENS_FACING_BACK
    private var mCameraPreview: Preview? = null
    private var mImageCapture: ImageCapture? = null
    private var mImageAnalyzer: ImageAnalysis? = null
    private var mCamera: Camera? = null

    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    private lateinit var cameraExecutor: ExecutorService

    /** Volume down button receiver used to trigger shutter */
    private val volumeDownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(KEY_EVENT_EXTRA, KeyEvent.KEYCODE_UNKNOWN)) {
                // When the volume down button is pressed, simulate a shutter button click
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    camera_capture_button.simulateClick()
                }
            }
        }
    }

    /**
     * We need a display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.mDisplayId) {
                Log.d(Constants.TAG, "Rotation changed: ${view.display.rotation}")
                mImageCapture?.targetRotation = view.display.rotation
                mImageAnalyzer?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    override fun onResume() {
        super.onResume()
        if (!Constants.hasPermissions(requireContext())) {
            Navigation.findNavController(
                    requireActivity(),
                    R.id.fragment_container).navigate(
                    CameraFragmentDirections.actionCameraToPermissions()
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        mLocalBroadcastManager.unregisterReceiver(volumeDownReceiver)
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?): View? =
            inflater.inflate(
                    R.layout.fragment_camera,
                    container,
                    false
            )

    private fun setGalleryThumbnail(uri: Uri) {
        photo_view_button.post {
            photo_view_button.setPadding(resources.getDimension(R.dimen.stroke_small).toInt())
            Glide.with(photo_view_button)
                    .load(uri)
                    .apply(RequestOptions.circleCropTransform())
                    .into(photo_view_button)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mContainer = view as ConstraintLayout
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(view.context)
        // Set up the intent filter that will receive events from our main activity
        val filter = IntentFilter().apply { addAction(KEY_EVENT_ACTION) }
        mLocalBroadcastManager.registerReceiver(volumeDownReceiver, filter)
        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)
        // Determine the output directory
        mOutputDirectory = Constants.getOutputDirectory(requireContext())
        // Wait for the views to be properly laid out
        view_finder.post {
            mDisplayId = view_finder.display.displayId
            updateCameraUi()
            bindCameraUseCases()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateCameraUi()
    }

    private fun setUpPinchToZoom() {
        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scale = mCamera!!.cameraInfo.zoomState.value!!.zoomRatio * detector.scaleFactor
                mCamera!!.cameraControl.setZoomRatio(scale)
                return true
            }
        }

        val scaleGestureDetector = ScaleGestureDetector(context, listener)
        view_finder.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            return@setOnTouchListener true
        }
    }

    private fun bindCameraUseCases() {
        val metrics = DisplayMetrics().also { view_finder.display.getRealMetrics(it) }
        Log.d(Constants.TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")
        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(Constants.TAG, "Preview aspect ratio: $screenAspectRatio")
        val rotation = view_finder.display.rotation
        // Bind the CameraProvider to the LifeCycleOwner
        val cameraSelector = CameraSelector.Builder().requireLensFacing(mDefaultLen).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            mCameraPreview = Preview.Builder()
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(rotation)
                    .build()
            // ImageCapture
            mImageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(rotation)
                    .build()
            // ImageAnalysis
            mImageAnalyzer = ImageAnalysis.Builder()
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(rotation)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                            Log.d(Constants.TAG, "Average luminosity: $luma")
                        })
                    }
            cameraProvider.unbindAll()
            try {
                mCamera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        mCameraPreview,
                        mImageCapture,
                        mImageAnalyzer
                )
                mCameraPreview?.setSurfaceProvider(view_finder.createSurfaceProvider(mCamera?.cameraInfo))
                setUpPinchToZoom()
            } catch (exc: Exception) {
                Log.e(Constants.TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /** Method used to re-draw the camera UI controls, called every time configuration changes. */
    private fun updateCameraUi() {
        lifecycleScope.launch(Dispatchers.IO) {
            mOutputDirectory.listFiles { file -> EXTENSION_WHITELIST.contains(file.extension.toUpperCase(Locale.ROOT)) }?.max()?.let { setGalleryThumbnail(Uri.fromFile(it)) }
        }
        // Listener for button used to capture photo
        camera_capture_button.setOnClickListener {
            mImageCapture?.let { imageCapture ->
                // Create output file to hold the image
                val photoFile = createFile(mOutputDirectory, FILENAME, PHOTO_EXTENSION)
                // Setup image capture metadata
                val metadata = Metadata().apply {
                    // Mirror image when using the front camera
                    isReversedHorizontal = mDefaultLen == CameraSelector.LENS_FACING_FRONT
                }
                // Create output options object which contains file + metadata
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                        .setMetadata(metadata)
                        .build()
                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(exc: ImageCaptureException) {
                                Log.e(Constants.TAG, "Photo capture failed: ${exc.message}", exc)
                            }

                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                                Log.d(Constants.TAG, "Photo capture succeeded: $savedUri")
                                activity?.intent?.data = savedUri
                                activity?.setResult(Activity.RESULT_OK, activity?.intent)
                                activity?.finish()
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    setGalleryThumbnail(savedUri)
                                }
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                                    requireActivity().sendBroadcast(Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri))
                                }
                                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(savedUri.toFile().extension)
                                MediaScannerConnection.scanFile(
                                        context,
                                        arrayOf(savedUri.toString()),
                                        arrayOf(mimeType)
                                ) { _, uri ->
                                    Log.d(Constants.TAG, "Image capture scanned into media store: $uri")
                                }
                            }
                        })
                // We can only change the foreground Drawable using API level 23+ API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mContainer.postDelayed({
                        mContainer.foreground = ColorDrawable(Color.WHITE)
                        mContainer.postDelayed({ mContainer.foreground = null }, ANIMATION_FAST_MILLIS)
                    },
                            ANIMATION_SLOW_MILLIS
                    )
                }
            }
        }

        camera_switch_button.setOnClickListener {
            mDefaultLen = if (CameraSelector.LENS_FACING_FRONT == mDefaultLen) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUseCases()
        }

        photo_view_button.setOnClickListener {
            if (true == mOutputDirectory.listFiles()?.isNotEmpty()) {
                Navigation.findNavController(
                        requireActivity(), R.id.fragment_container
                ).navigate(CameraFragmentDirections
                        .actionCameraToGallery(mOutputDirectory.absolutePath))
            }
        }
    }


    private class LuminosityAnalyzer(listener: LumaListener? = null) : ImageAnalysis.Analyzer {
        private val frameRateWindow = 8
        private val frameTimestamps = ArrayDeque<Long>(5)
        private val listeners = ArrayList<LumaListener>().apply { listener?.let { add(it) } }
        private var lastAnalyzedTimestamp = 0L
        var framesPerSecond: Double = -1.0
            private set

        /**
         * Used to add listeners that will be called with each luma computed
         */
        fun onFrameAnalyzed(listener: LumaListener) = listeners.add(listener)

        /**
         * Helper extension function used to extract a byte array from an image plane buffer
         */
        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {
            // If there are no listeners attached, we don't need to perform analysis
            if (listeners.isEmpty()) {
                image.close()
                return
            }

            // Keep track of frames analyzed
            val currentTime = System.currentTimeMillis()
            frameTimestamps.push(currentTime)
            // Compute the FPS using a moving average
            while (frameTimestamps.size >= frameRateWindow) frameTimestamps.removeLast()
            val timestampFirst = frameTimestamps.peekFirst() ?: currentTime
            val timestampLast = frameTimestamps.peekLast() ?: currentTime
            framesPerSecond = 1.0 / ((timestampFirst - timestampLast) /
                    frameTimestamps.size.coerceAtLeast(1).toDouble()) * 1000.0
            // Analysis could take an arbitrarily long amount of time
            // Since we are running in a different thread, it won't stall other use cases
            lastAnalyzedTimestamp = frameTimestamps.first
            // Since format in ImageAnalysis is YUV, image.planes[0] contains the luminance plane
            val buffer = image.planes[0].buffer
            // Extract image data from callback object
            val data = buffer.toByteArray()
            // Convert the data into an array of pixel values ranging 0-255
            val pixels = data.map { it.toInt() and 0xFF }
            // Compute average luminance for the image
            val luma = pixels.average()
            // Call all listeners with new value
            listeners.forEach { it(luma) }
            image.close()
        }
    }
}
