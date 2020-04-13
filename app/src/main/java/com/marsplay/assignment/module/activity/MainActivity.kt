package com.marsplay.assignment.module.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.marsplay.assignment.R
import com.marsplay.assignment.constants.Constants
import com.marsplay.assignment.constants.Constants.Companion.Image_Request_Code
import com.marsplay.assignment.module.camera.CamerActivity
import com.marsplay.assignment.module.fragments.viewmodels.MainViewModel
import com.marsplay.assignment.utiles.ImagePickerDialog
import com.marsplay.assignment.utiles.ImageUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import javax.inject.Inject

class MainActivity : BaseActivity() {

    @Inject
    lateinit var viewModel: MainViewModel

    private var mFilePathUri: Uri? = null

    private lateinit var mImagePickerDialog: ImagePickerDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setViewModel()
        initViews()
    }

    private fun initViews() {
        // Adding click listener to Choose image button.¬
        btChooseImage!!.setOnClickListener {
            getImagePicker()
        }
        // Adding click listener to Upload image button.
        btUploadImage!!.setOnClickListener {
            // Calling method to upload selected image on Firebase storage.
            viewModel.uploadImageFileToFirebaseStorage(
                    pFilePathUri = mFilePathUri,
                    pImageNameEditText = imageNameEditText.text.toString()
            )
        }
        btDisplayImages!!.setOnClickListener {
            val intent = Intent(this@MainActivity, DisplayImagesActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Image_Request_Code
                && resultCode == Activity.RESULT_OK
                && data != null
                && data.data != null
        ) {
            mFilePathUri = data.data
            try { // Getting selected image into Bitmap.
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, mFilePathUri)
                showImageView!!.setImageBitmap(bitmap)
                showImageView.visibility= View.VISIBLE
                btChooseImage!!.text = getString(R.string.image_selected)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }

    private fun setViewModel() {
        viewModel = ViewModelProvider(this, mViewModelFactory).get(MainViewModel::class.java)
    }

    fun openGallery() {
        ImageUtils.newInstance().openGallery(this)
    }

    fun openCamera() {
        startActivityForResult(Intent(this, CamerActivity::class.java), Constants.Image_Request_Code)
    }

    private fun getImagePicker() {
        when (::mImagePickerDialog.isInitialized) {
            false -> mImagePickerDialog = ImagePickerDialog.newInstance()
        }
        when (!mImagePickerDialog.isAdded) {
            true -> mImagePickerDialog.show(supportFragmentManager, mImagePickerDialog.javaClass.simpleName)
        }
    }
}