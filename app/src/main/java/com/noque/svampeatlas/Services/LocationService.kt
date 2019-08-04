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
import java.lang.Exception
import java.lang.ref.WeakReference
import java.util.logging.Handler


interface LocationServiceDelegate {
    fun locationRetrieved(location: Location)
    fun locationRetrievalError(exception: Exception)
}


class LocationService(private val context: Context, private val activity: Activity) {

    private var locationClient: FusedLocationProviderClient? = null
    private var lastLocation: Location? = null

    private var listener: LocationServiceDelegate? = null

    private val permissionGranted: Boolean get() {
        if(ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            return false
        } else {
            return true
        }
    }

    fun setListener(locationServiceDelegate: LocationServiceDelegate) {
        listener = locationServiceDelegate
    }


    fun start() {
        locationClient = LocationServices.getFusedLocationProviderClient(context)

        if (permissionGranted) {
            getLocation()
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {

        locationClient?.lastLocation

        val locationRequest = LocationRequest.create()
            locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest.interval = 10


        val callBack = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (locationResult == null) {
                    return
                }

                lastLocation = locationResult.lastLocation


                if (locationResult.lastLocation.hasAccuracy() && locationResult.lastLocation.accuracy <= 100) {
                    listener?.locationRetrieved(locationResult.lastLocation)
                    locationClient?.removeLocationUpdates(this)
                }
            }
        }

        android.os.Handler().postDelayed(Runnable {
            if (listener != null) {
                lastLocation?.let {
                    listener?.locationRetrieved(it)
                }
            }
        }, 100)

        locationClient?.requestLocationUpdates(locationRequest, callBack, null)
    }


    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
//            Snackbar.make(view!!, "Rationale", Snackbar.LENGTH_SHORT).setAction("Vil du godkende?", View.OnClickListener { startLocationPermissionRequest() })
        } else {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION), 21)
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 21) {
            if ((grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            } else {

//                View.OnClickListener {
//                    // Build intent that displays the App settings screen.
//                    val intent = Intent()
//                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                    val uri = Uri.fromParts("package",
//                        BuildConfig.APPLICATION_ID, null)
//                    intent.data = uri
//                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                    startActivity(intent)
//                })

                // SHOW ERROR ON MAP
            }
        }
    }
}