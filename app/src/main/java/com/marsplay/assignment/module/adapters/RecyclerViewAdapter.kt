package com.marsplay.assignment.module.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.marsplay.assignment.R
import com.marsplay.assignment.model.response.ImageUploadInfo
import kotlinx.android.synthetic.main.recyclerview_items.view.*

class RecyclerViewAdapter(var context: Context, var mImageUploadInfoList: List<ImageUploadInfo>) : RecyclerView.Adapter<ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_items, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val uploadInfo = mImageUploadInfoList[position]
        holder.mItemView.imageName.text = uploadInfo.mImageName
        //Loading image from Glide library.
        Glide.with(context).load(uploadInfo.mImageURL).into(holder.mItemView.ivThumbnail)
    }


    override fun getItemCount(): Int {
        return mImageUploadInfoList.size
    }

}

class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var mItemView = itemView
}