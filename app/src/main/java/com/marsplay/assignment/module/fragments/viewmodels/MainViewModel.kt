package com.marsplay.assignment.module.fragments.viewmodels

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.marsplay.assignment.R
import com.marsplay.assignment.application.ApplicationShared
import com.marsplay.assignment.constants.Constants
import com.marsplay.assignment.model.response.ImageUploadInfo
import com.marsplay.assignment.utiles.ImageUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


class MainViewModel @Inject constructor(pApplication: Application) : AndroidViewModel(pApplication) {

    private lateinit var mStorageReference: StorageReference
    private lateinit var mDatabaseReference: DatabaseReference

    private var mApplication = pApplication as ApplicationShared

    private var mListOfUsers = MutableLiveData<ArrayList<ImageUploadInfo>>()

    fun getUsersList() = mListOfUsers

    fun uploadImageFileToFirebaseStorage(pFilePathUri: Uri?, pImageNameEditText: String) {
        initFireBase()
        if (pFilePathUri != null) {
            mApplication.showProgress()
            val storageReference2nd = mStorageReference.child(Constants.Storage_Path + System.currentTimeMillis() + "." + ImageUtils.newInstance().getFileExtension(mApplication.getmActivity()!!, pFilePathUri))
            storageReference2nd.putFile(pFilePathUri)
            val observable = Observable.just(storageReference2nd.activeUploadTasks)
            observable.subscribeOn(Schedulers.io()).let { it ->
                it.observeOn(AndroidSchedulers.mainThread())
                it.subscribe { it ->
                    it[0].addOnSuccessListener {
                        val tempImageName = pImageNameEditText.trim { it <= ' ' }
                        mApplication.hideProgressDialog()
                        storageReference2nd.downloadUrl.addOnSuccessListener {
                            when (it == null) {
                                false -> {
                                    val imageUploadInfo = ImageUploadInfo(tempImageName, it.toString())
                                    // Getting image upload ID.
                                    val imageUploadId = mDatabaseReference.push().key
                                    // Adding image upload id s child element into databaseReference.
                                    mDatabaseReference.child(imageUploadId!!).setValue(imageUploadInfo)
                                    Toast.makeText(mApplication.getmActivity(), mApplication.getmActivity()?.getString(R.string.image_upload_success), Toast.LENGTH_LONG).show()
                                }
                            }
                        }.addOnFailureListener { e ->
                            Toast.makeText(mApplication.getmActivity(), e.message, Toast.LENGTH_LONG).show()
                        }
                    }.addOnFailureListener { exception ->
                        // If something goes wrong .
                        mApplication.hideProgressDialog()
                        Toast.makeText(mApplication.getmActivity(), exception.message, Toast.LENGTH_LONG).show()
                    }.addOnProgressListener {
                        // Setting progressDialog Title.
                    }
                }
            }
        } else {
            Toast.makeText(mApplication.getmActivity(), mApplication.getmActivity()?.getString(R.string.please_select_image), Toast.LENGTH_LONG).show()
        }
    }

    private fun initFireBase() {
        when (::mStorageReference.isInitialized) {
            false -> mStorageReference = FirebaseStorage.getInstance().reference
        }
        when (::mDatabaseReference.isInitialized) {
            false -> mDatabaseReference = FirebaseDatabase.getInstance().getReference(Constants.Database_Path)
        }
    }

    fun downloadImagesFromDataSource() {
        initFireBase()
        mApplication.showProgress()
        mDatabaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val observable = Observable.just(snapshot)
                observable.subscribeOn(Schedulers.io()).let { it ->
                    it.observeOn(AndroidSchedulers.mainThread())
                    it.subscribe {
                        val tempListOfUsers = ArrayList<ImageUploadInfo>()
                        for (postSnapshot in it.children) {
                            val imageUploadInfo = postSnapshot.getValue(ImageUploadInfo::class.java)!!
                            tempListOfUsers.add(imageUploadInfo)
                        }
                        mListOfUsers.postValue(tempListOfUsers)
                        mApplication.hideProgressDialog()
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                mApplication.hideProgressDialog()
            }
        })
    }
}