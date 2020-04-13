package com.marsplay.assignment.constants

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.marsplay.assignment.R
import com.marsplay.assignment.module.camera.fragments.PERMISSIONS_REQUIRED
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

interface Constants {
    companion object {
        var Storage_Path = "images_upload/"
        var Image_Request_Code = 7
        const val Database_Path = "image_upload_db"
        const val TAG = "CameraXBasic"
        const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val PHOTO_EXTENSION = ".jpg"
        const val RATIO_4_3_VALUE = 4.0 / 3.0
        const val RATIO_16_9_VALUE = 16.0 / 9.0
        /** Helper function used to create a timestamped file */
        fun createFile(baseFolder: File, format: String, extension: String) =
                File(baseFolder, SimpleDateFormat(format, Locale.US)
                        .format(System.currentTimeMillis()) + extension)

        fun hasPermissions(context: Context) = PERMISSIONS_REQUIRED.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }
}