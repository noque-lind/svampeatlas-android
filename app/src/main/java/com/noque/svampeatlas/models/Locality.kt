package com.noque.svampeatlas.models

import androidx.room.Embedded
import androidx.room.Ignore
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class Locality(
    @SerializedName("_id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("kommune") val municipality: String?,
    @SerializedName("decimalLatitude") val latitude: Double,
    @SerializedName("decimalLongitude") val longitude: Double,

    @Embedded(prefix = "geoname_")
    val geoName: GeoName? = null)
{
    companion object {
        fun fromGson(value: String): Locality {
            return Gson().fromJson(value, Locality::class.java)
        }
    }

    constructor(id: Int, name: String, municipality: String?, latitude: Double, longitude: Double): this(id, name, municipality, latitude, longitude, null)
    val location: LatLng get() {return LatLng(latitude, longitude)}


    fun toGson(): String {
        val gson = Gson()
        return gson.toJson(this)
    }
}
