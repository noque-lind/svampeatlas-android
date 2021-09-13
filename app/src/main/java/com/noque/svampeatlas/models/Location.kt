package com.noque.svampeatlas.models

import androidx.room.Embedded
import com.google.android.gms.maps.model.LatLng
import java.util.*

data class Location(
    val date: Date,
    val latLng: LatLng,
    val accuracy: Float)