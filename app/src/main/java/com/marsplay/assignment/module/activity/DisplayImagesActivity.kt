package com.marsplay.assignment.module.activity

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.marsplay.assignment.R
import com.marsplay.assignment.model.response.ImageUploadInfo
import com.marsplay.assignment.module.adapters.RecyclerViewAdapter
import com.marsplay.assignment.module.fragments.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.activity_display_images.*
import java.util.*
import javax.inject.Inject

class DisplayImagesActivity : BaseActivity() {

    @Inject
    lateinit var viewModel: MainViewModel
    private lateinit var mAdapter: RecyclerView.Adapter<*>
    private var mImageList: MutableList<ImageUploadInfo> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_images)
        setViewModel()
        setAdapter()
    }

    private fun setAdapter() {
        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.layoutManager = GridLayoutManager(this@DisplayImagesActivity,2)
        mAdapter = RecyclerViewAdapter(applicationContext, mImageList)
        recyclerView!!.adapter = mAdapter
    }

    private fun setViewModel() {
        viewModel = ViewModelProvider(this, mViewModelFactory).get(MainViewModel::class.java)
        viewModel.getUsersList().observe(this, androidx.lifecycle.Observer {
            mImageList.clear()
            mImageList.addAll(it)
            mAdapter.notifyDataSetChanged()
        })
        viewModel.downloadImagesFromDataSource()
    }
}