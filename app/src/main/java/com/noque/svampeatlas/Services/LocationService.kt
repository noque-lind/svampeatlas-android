package com.noque.svampeatlas.Services

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.noque.svampeatlas.Model.AppError
import com.noque.svampeatlas.R
import java.lang.Exception
import java.lang.ref.WeakReference
import java.util.logging.Handler





class LocationService(private val context: Context?, private val activity: Activity?) {

    companion object {
        val TAG = "LocationService"
        val REQUESTCODE = 210
    }

    sealed class Error(title: String, message: String): AppError(title, message) {
        class PermissionDenied(context: Context): Error(context.getString(R.string.locationservice_error_permissions_title), context.getString(R.string.locationservice_error_permissions_message))
    }

    interface Listener {
        fun locationRetrieved(location: Location)
        fun locationRetrievalError(error: Error)
        fun requestPermission(permissions: Array<out String>, requestCode: Int)
    }

    private var locationClient: FusedLocationProviderClient? = null
    private var lastLocation: Location? = null
    private var listener: Listener? = null

    private val permissionGranted: Boolean get() {
        return context?.let {
            (ContextCompat.checkSelfPermission(it, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)} ?: false
    }

    private val locationCallback = object: LocationCallback() {
        override fun onLocationResult(result: LocationResult?) {
            result?.lastLocation?.let {
                lastLocation = it

                if (it.accuracy <= 60) {
                    lastLocation = null
                    listener?.locationRetrieved(it)
                    locationClient?.removeLocationUpdates(this)
                    locationClient = null
                }

            }
        }
    }


    fun setListener(locationServiceDelegate: Listener?) {
        listener = locationServiceDelegate
    }

    fun start() {
        Log.d(TAG, "Starting")
        context?.let { locationClient = LocationServices.getFusedLocationProviderClient(context) }

        if (permissionGranted) {
            getLocation()
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = 10


        android.os.Handler().postDelayed(Runnable {
            if (listener != null) {
                lastLocation?.let {
                    listener?.locationRetrieved(it)
                }
            }
        }, 100)

        locationClient?.requestLocationUpdates(locationRequest, locationCallback, null)
    }


    private fun requestPermissions() {
            Log.d(TAG, "Requesting permissions")
            listener?.requestPermission(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), REQUESTCODE)
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUESTCODE) {
            Log.d(TAG,"OnRequestPermissionsResult called")

            if ((grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED)) {
                Log.d(TAG,"Permission has now been granted")
                getLocation()
            } else {
                context?.let {
                    listener?.locationRetrievalError(Error.PermissionDenied(context))
                }
            }
        }
    }
}