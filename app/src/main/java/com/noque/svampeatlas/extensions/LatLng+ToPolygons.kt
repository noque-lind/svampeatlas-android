package com.noque.svampeatlas.extensions

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


fun LatLng.toRectanglePolygon(radius: Double): List<LatLng> {
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

fun LatLng.toBounds(radius: Double): LatLngBounds {
    val radiusInMeters = Math.sqrt(2.0) * radius

    val southWestCorner = SphericalUtil.computeOffset(this, radiusInMeters, 225.0)
    val northeast = SphericalUtil.computeOffset(this, radiusInMeters, 45.0)
    return LatLngBounds(southWestCorner, northeast)
}

fun LatLng.toCircularPolygon(radius: Double, numberOfSegments: Int = 20): List<LatLng> {
        val coordinates = mutableListOf<LatLng>()

        val degreeLatitude = Math.toRadians(this.latitude)
        val degreeLongitude = Math.toRadians(this.longitude)
        val radiusDividedByEarthRadius = radius / 6378137


        while (coordinates.count() < numberOfSegments) {
            val bearing = 2 * Math.PI * coordinates.count() / numberOfSegments.toDouble()
            val offsetLatitude = asin((sin(degreeLatitude) * cos(radiusDividedByEarthRadius)) + cos(degreeLatitude) * sin(radiusDividedByEarthRadius) * cos(bearing))
            val offsetLongitude = degreeLongitude + atan2(sin(bearing) * sin(radiusDividedByEarthRadius) * cos(degreeLatitude), cos(radiusDividedByEarthRadius) - sin(degreeLatitude) * sin(offsetLatitude))
            coordinates.add(LatLng(Math.toDegrees(offsetLatitude), Math.toDegrees(offsetLongitude)))
        }

        coordinates.add(coordinates.first())
    return coordinates
}