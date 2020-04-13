package com.marsplay.assignment.utiles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.marsplay.assignment.R
import com.marsplay.assignment.module.activity.MainActivity
import kotlinx.android.synthetic.main.image_picker_dialog.*

class ImagePickerDialog : DialogFragment() {

    companion object {
        fun newInstance() = ImagePickerDialog()
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.image_picker_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        btCamera.setOnClickListener {
            when (activity) {
                is MainActivity -> {
                    (activity as MainActivity).openCamera()
                    dismiss()
                }
            }
        }
        btGallery.setOnClickListener {
            when (activity) {
                is MainActivity -> {
                    (activity as MainActivity).openGallery()
                    dismiss()
                }
            }
        }
    }
}