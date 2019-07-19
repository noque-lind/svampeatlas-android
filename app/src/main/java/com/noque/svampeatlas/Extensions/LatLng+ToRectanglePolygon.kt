package com.noque.svampeatlas.Extensions

import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polygon
import com.google.maps.android.SphericalUtil
import com.google.maps.android.data.Geometry


fun LatLng.toRectanglePolygon(radius: Double): List<LatLng> {
    val radiusInMeters = Math.sqrt(2.0) * radius

    val southWestCorner = SphericalUtil.computeOffset(this, radiusInMeters, 225.0)
    val northeast = SphericalUtil.computeOffset(this, radiusInMeters, 45.0)

val bound = LatLngBounds(southWestCorner, northeast)

    val coordinates = mutableListOf(
        LatLng(northeast.latitude, southWestCorner.longitude),
        northeast,
        LatLng(southWestCorner.latitude, northeast.longitude),
        southWestCorner
    )

    coordinates.add(coordinates.first())
    return coordinates
}