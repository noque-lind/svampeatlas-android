package com.noque.svampeatlas.models

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName

data class Locality(
    @SerializedName("_id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("kommune") val municipality: String?,
    @SerializedName("decimalLatitude") private val latitude: Double,
    @SerializedName("decimalLongitude") private val longitude: Double) {
    val location: LatLng get() {return LatLng(latitude, longitude)}
}
