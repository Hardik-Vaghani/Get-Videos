package com.hardik.getvideos.ui

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.hardik.getvideos.MainActivity
import com.hardik.getvideos.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoFragment : Fragment() {

    companion object { fun newInstance() = VideoFragment() }

    private val TAG = VideoFragment::class.java.simpleName

    private lateinit var videoView: VideoView
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var videoPickerLauncher: ActivityResultLauncher<Intent>
//    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoRecorderLauncher: ActivityResultLauncher<Intent>
    private lateinit var videoFile: File
    private lateinit var videoUri: Uri

    private lateinit var viewModel: VideoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the ActivityResultLauncher here
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var allGranted = true
            permissions.entries.forEach { entry ->
                if (!entry.value) {
                    allGranted = false
                }
            }

            if (allGranted) {
                // All permissions are granted, proceed with your functionality
                Toast.makeText(requireContext(), "All permissions granted ✔️", Toast.LENGTH_SHORT).show()
                showImageSourceDialog()
            } else {
                // Handle the case where permissions are not granted
                Toast.makeText(requireContext(), "Permissions denied ✖️", Toast.LENGTH_SHORT).show()
            }
        }

        videoPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
//                    Glide.with(this)
//                        .load(uri)
//                        .into(imageView)
                    // Play the existing video
                    playVideo(uri)
                    Log.e(TAG, "Selected video URI: $uri")
                }
            }
        }

        videoRecorderLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Play the recorded video // Load the captured image into the ImageView
//                Glide.with(this)
//                    .load(photoUri) // Use the URI we stored
//                    .into(imageView)
                videoUri = result.data?.data ?: Uri.EMPTY
                playVideo(videoUri)
                Log.e(TAG, "Captured video URI: $videoUri")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_video, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(VideoViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        videoView = view.findViewById(R.id.video_view)
        val imgBtn = view.findViewById<AppCompatImageButton>(R.id.img_btn)
        imgBtn.setOnClickListener {
            checkPermissions() // Just check permissions here
        }
    }

    private fun checkPermissions() {
        val permissionsNeeded = arrayOf(
            Manifest.permission.CAMERA,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES // For Android 13 and higher
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            }
        )

        val permissionsToRequest = permissionsNeeded.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest)
        } else {
            // All permissions are granted, proceed with your functionality
            showImageSourceDialog()
        }
    }

    private fun showImageSourceDialog() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Select Video Source")
            .setItems(arrayOf("Camera", "Gallery")) { dialog, which ->
                when (which) {
                    0 -> onCameraSelected() // Capture image
                    1 -> onGallerySelected() // Select image / video
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun onGallerySelected() {
//        openImagePicker()
        openVideoPicker()
    }

    private fun onCameraSelected() {
        dispatchRecordVideoIntent() //  dispatchTakePictureIntent()
    }

    private fun dispatchRecordVideoIntent() {

        // Create an image file
        videoFile = createImageFile()

        // Get the image URI
        videoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            videoFile
        )

        // Launch camera intent
//        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
//            putExtra(MediaStore.EXTRA_OUTPUT, videoUri) // Set the URI for the image
//        }
        // Launch camera intent for Video recording
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, videoUri)
            putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
        }

        videoRecorderLauncher.launch(intent)
    }

    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val storageDir: File? =
            // Using getExternalFilesDir for app-specific storage
//            requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES) // /storage/emulated/0/Android/data/<your.package.name>/files/
            // Using getExternalStoragePublicDirectory for public storage
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) // /storage/emulated/0/Movies/

        // Create a permanent file (not temporary)
        return File(storageDir, "VIDEO_$timeStamp.mp4").apply {
            Log.d(TAG, "Video file created at: $absolutePath")
        }
    }

    private fun openVideoPicker(){
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        videoPickerLauncher.launch(intent)
    }

    private fun playVideo(videoUri: Uri) {
        videoView.setVideoURI(videoUri)

        val mediaController = MediaController(requireContext())
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        videoView.setOnPreparedListener { mediaPlayer ->
//            mediaPlayer.setOnVideoSizeChangedListener { _, _, _ ->
            // Start playback directly when the video is prepared, if you don't uncomment above function 'setOnVideoSizeChangedListener{}'
                videoView.start()
//            }
        }

        videoView.setOnErrorListener { mp, what, extra ->
            Log.e(TAG, "Error playing video: what=$what, extra=$extra")
            Toast.makeText(requireContext(), "Error playing video", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        videoPickerLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopVideoPlayback()
    }

    private fun stopVideoPlayback() {
        if (videoView.isPlaying) {
            videoView.stopPlayback()
        }
    }
}