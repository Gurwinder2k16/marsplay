package com.marsplay.assignment.utiles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.marsplay.assignment.R

class ProgressBarDialog : DialogFragment() {

    companion object {
        fun newInstance() = ProgressBarDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.progress_dialog, container, false)
    }
}