package com.noque.svampeatlas.extensions

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


fun LatLng.toRectanglePolygon(radius: Int): List<LatLng> {
    val radiusInMeters = Math.sqrt(2.0) * radius

    val southWestCorner = SphericalUtil.computeOffset(this, radiusInMeters, 225.0)
    val northeast = SphericalUtil.computeOffset(this, radiusInMeters, 45.0)

    val coordinates = mutableListOf(
        LatLng(northeast.latitude, southWestCorner.longitude),
        northeast,
        LatLng(southWestCorner.latitude, northeast.longitude),
        southWestCorner
    )

    coordinates.add(coordinates.first())
    return coordinates
}

fun LatLng.toBounds(radius: Int): LatLngBounds {
    val radiusInMeters = Math.sqrt(2.0) * radius

    val southWestCorner = SphericalUtil.computeOffset(this, radiusInMeters, 225.0)
    val northeast = SphericalUtil.computeOffset(this, radiusInMeters, 45.0)
    return LatLngBounds(southWestCorner, northeast)
}

fun LatLng.toCircularPolygon(radius: Int, numberOfSegments: Int = 15): List<LatLng> {
        val coordinates = mutableListOf<LatLng>()

        val radiansLatitude = Math.toRadians(this.latitude)
        val radiansLongitude = Math.toRadians(this.longitude)
        val radiusDividedByEarthRadius = radius.toDouble() / 6378137

        while (coordinates.count() < numberOfSegments) {
            val bearing = 2 * Math.PI * coordinates.count() / numberOfSegments.toDouble()
            val offsetLatitude = asin((sin(radiansLatitude) * cos(radiusDividedByEarthRadius)) + cos(radiansLatitude) * sin(radiusDividedByEarthRadius) * cos(bearing))
            val offsetLongitude = radiansLongitude + atan2(sin(bearing) * sin(radiusDividedByEarthRadius) * cos(radiansLatitude), cos(radiusDividedByEarthRadius) - sin(radiansLatitude) * sin(offsetLatitude))
            coordinates.add(LatLng(Math.toDegrees(offsetLatitude), Math.toDegrees(offsetLongitude)))
        }

        coordinates.add(coordinates.first())
    return coordinates
}


