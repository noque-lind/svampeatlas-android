package com.noque.svampeatlas.services

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.R


class LocationService(private val applicationContext: Context) {

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
        return (ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
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
        if (locationClient != null) return
        locationClient = LocationServices.getFusedLocationProviderClient(applicationContext)

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
                    listener?.locationRetrievalError(Error.PermissionDenied(applicationContext))
            }
        }
    }
}