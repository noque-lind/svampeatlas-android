package com.noque.svampeatlas.models

import android.content.Context
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.toDatabaseName
import com.noque.svampeatlas.view_models.NewObservationViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*

sealed class NewObservationError(title: Int, message: Int) :
    AppError2(title, message, null) {
    object NoMushroomError: NewObservationError(R.string.error_newObservation_missingInformation_title, R.string.error_newObservation_noMushroom_message)
    object NoSubstrateError: NewObservationError(R.string.error_newObservation_missingInformation_title, R.string.error_newObservation_noSubstrategroup_message)
    object NoVegetationTypeError: NewObservationError(R.string.error_newObservation_missingInformation_title, R.string.error_newObservation_noVegetationType_message)
    object NoLocationDataError: NewObservationError(R.string.error_newObservation_noCoordinates_title, R.string.error_newObservation_noCoordinates_message)
}

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
    var determinationNotes: String? = null,

    var hostIDs: List<Int> = listOf(),
    var images: List<String> = listOf()) {

    fun isComplete(): Boolean {
        return locality != null && substrate != null && vegetationType != null && coordinate != null && species != null
    }

    fun createJSON(includeTaxon: Boolean): Result<JSONObject, NewObservationError> {
        val substrate = substrate
        val vegetationType = vegetationType
        val location = coordinate
        val locality = locality
        val species = species

        if (substrate == null) return Result.Error(NewObservationError.NoSubstrateError)
        if (vegetationType == null) return Result.Error(NewObservationError.NoVegetationTypeError)
        if (location == null || locality == null) return Result.Error(NewObservationError.NoLocationDataError)
            return Result.Success(JSONObject().apply {
                put("observationDate", (observationDate.toDatabaseName()))
                put("os", "Android")
                put("browser", "Native App")
                put("substrate_id", substrate.id)
                put("vegetationtype_id", vegetationType.id)
                put("associatedOrganisms", JSONArray().apply {
                    hostIDs.map { JSONObject().put("_id", it) }.forEach { this.put(it) }
                })
                put("ecologynote", ecologyNote)
                put("note", note)
                put("decimalLatitude", location.latLng.latitude)
                put("decimalLongitude", location.latLng.longitude)
                put("accuracy", location.accuracy)

                if (locality.geoName != null) {
                    put("geonameId", locality.geoName.geonameId)
                    put(
                        "geoname", JSONObject()
                            .put("geonameId", locality.geoName.geonameId)
                            .put("name", locality.geoName.name)
                            .put("adminName1", locality.geoName.adminName1)
                            .put("lat", locality.geoName.lat)
                            .put("lng", locality.geoName.lng)
                            .put("countryName", locality.geoName.countryName)
                            .put("countryCode", locality.geoName.countryCode)
                            .put("fcodeName", locality.geoName.fcodeName)
                            .put("fclName", locality.geoName.fclName)
                    )
                } else {
                    put("locality_id", locality.id)
                }


                if (includeTaxon) {
                    if (species == null) return Result.Error(NewObservationError.NoMushroomError)
                    put("determination",
                        JSONObject()
                            .put("confidence", confidence ?: DeterminationConfidence.CONFIDENT.databaseName)
                            .put("taxon_id", species.id)
                            .put("notes", determinationNotes ?: ""))
                }
        } )
    }
}