package com.marsplay.assignment.network.networkutil.apiConfig

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log

class NetworkUtilites {
    companion object {
        var connectivityManager: ConnectivityManager? = null
        var connected = false
        fun isInternetAvailable(context: Context): Boolean {
            try {
                connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val networkInfo = connectivityManager!!.activeNetworkInfo
                connected = networkInfo != null && networkInfo.isAvailable && networkInfo.isConnected
                return connected

            } catch (e: Exception) {
                Log.v("connectivity", e.toString())
            }
            return connected
        }
    }
}