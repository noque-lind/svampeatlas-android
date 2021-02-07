package com.noque.svampeatlas.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*


@Entity(tableName = "notes")
data class NewObservation(
    @PrimaryKey
    val creationDate: Date = Date(),
    var observationDate: Date,
    @Embedded(prefix = "taxon_")
    var species: Mushroom? = null,
    @Embedded(prefix = "locality_")
    var locality: Locality? = null,
    @Embedded(prefix = "substrate_")
    var substrate: Substrate? = null,
    @Embedded(prefix = "vegetation_type_")
    var vegetationType: VegetationType? = null,
    @Embedded(prefix = "coordinate_")
    var coordinate: Location? = null,
    var ecologyNote: String? = null,
    var note: String? = null,
    var confidence: String? = null,

    var hostIDs: List<Int> = listOf(),
    var images: List<String> = listOf()) {
}