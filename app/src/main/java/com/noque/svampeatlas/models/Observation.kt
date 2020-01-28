package com.noque.svampeatlas.models

import androidx.room.Database
import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import com.noque.svampeatlas.extensions.Date
import com.noque.svampeatlas.extensions.toISO8601
import com.noque.svampeatlas.services.RoomService
import org.intellij.lang.annotations.Subst
import java.util.*

data class Observation(
    @SerializedName("_id") private val _id: Int = 0,
    @SerializedName("observationDate") private val _observationDate: String?,
    @SerializedName("ecologynote") private val _ecologyNote: String?,
    @SerializedName("note") private val _note: String?,
    @SerializedName("geom") private val _geom: Geom,
    @SerializedName("DeterminationView") private val _determinationView: DeterminationView?,
    @SerializedName("PrimaryDetermination") private val _primaryDetermination: PrimaryDeterminationView?,
    @SerializedName("Images") private val _observationImages: List<ObservationImage>?,
    @SerializedName("PrimaryUser") private val _primaryUser: PrimaryUser?,
    @SerializedName("Locality") private val _locality: Locality?,
    @SerializedName("GeoNames") private val geoName: GeoName?,
    @SerializedName("Forum") private val _forum: MutableList<Forum>,
    @SerializedName("vegetationtype_id") private val vegetationTypeID: Int?,
    @SerializedName("substrate_id") private val substrateID: Int?

) {

    enum class ValidationStatus {
        APPROVED,
        VERIFYING,
        REJECTED,
        UNKNOWN
    }


    data class SpeciesProperties(val id: Int, val name: String)


    val id: Int
        get() {
            return _id
        }
    val coordinate: LatLng
        get() {
            return LatLng(_geom.coordinates.last(), _geom.coordinates.first())
        }
    val date: Date?
        get() {
            return Date(_observationDate)
        }
    val observationBy: String?
        get() {
            return _primaryUser?.profile?.name
        }
    val note: String?
        get() {
            return _note
        }
    val ecologyNote: String?
        get() {
            return _ecologyNote
        }
    val location: String?
        get() {
            if (geoName != null) {
                return "${geoName.countryName}, ${geoName.name}"
            } else {
                return _locality?.name
            }
        }
    val validationStatus: ValidationStatus
        get() {
            if (_determinationView?.determinationScore != null && _determinationView.determinationScore >= 80) {
                return ValidationStatus.APPROVED
            } else if (_determinationView?.determinationValidation != null) {
                return when (_determinationView.determinationValidation) {
                    "Afvist" -> ValidationStatus.REJECTED
                    "Godkendt" -> ValidationStatus.APPROVED
                    "Valideres" -> ValidationStatus.VERIFYING
                    else -> ValidationStatus.UNKNOWN
                }
            } else if (_primaryDetermination?.score != null && _primaryDetermination.score >= 80) {
                return ValidationStatus.APPROVED
            } else if (_primaryDetermination?.validation != null) {
                return when (_primaryDetermination.validation) {
                    "Afvist" -> ValidationStatus.REJECTED
                    "Godkendt" -> ValidationStatus.APPROVED
                    "Valideres" -> ValidationStatus.VERIFYING
                    else -> ValidationStatus.UNKNOWN
                }
            } else {
                return ValidationStatus.UNKNOWN
            }
        }

    val speciesProperties: SpeciesProperties
        get() {
            return if (_determinationView != null) {
                SpeciesProperties(
                    _determinationView.taxonID,
                    _determinationView.vernacularNameDK ?: _determinationView.fullName
                )
            } else if (_primaryDetermination != null) {
                SpeciesProperties(
                    _primaryDetermination.taxon.acceptedTaxon.id,
                    _primaryDetermination.taxon.acceptedTaxon.vernacularNameDK?.vernacularname_dk
                        ?: _primaryDetermination.taxon.acceptedTaxon.fullName
                )
            } else {
                SpeciesProperties(0, "")
            }
        }

    val images: List<Image>
        get() {
            return _observationImages?.map {
                Image(
                    0,
                    0,
                    "https://svampe.databasen.org/uploads/${it.name}.JPG",
                    null
                )
            } ?: listOf()
        }
    val comments: List<Comment>?
        get() {
            return _forum.map {
                Comment(
                    it.id,
                    it.createdAt,
                    it.content,
                    it.user.profile.name,
                    it.user.profile.initials,
                    it.user.profile.facebook
                )
            }
        }


    fun addComment(comment: Comment) {
        _forum.add(Forum(comment.id, comment.date?.toISO8601() ?: "", comment.content, PrimaryUser(Profile(comment.commenterName, comment.initials ?: "", comment.commenterProfileImageURL))))
    }
}


data class Geom(val coordinates: List<Double>)

data class DeterminationView(
    @SerializedName("taxon_id") val taxonID: Int,
    @SerializedName("taxon_FullName") val fullName: String,
    @SerializedName("taxon_vernacularname_dk") val vernacularNameDK: String?,
    @SerializedName("redlistStatus") val redlistStatus: String?,
    @SerializedName("determination_validation") val determinationValidation: String?,
    @SerializedName("determination_score") val determinationScore: Int?
)

data class PrimaryDeterminationView(
    @SerializedName("score") val score: Int?,
    @SerializedName("validation") val validation: String?,
    @SerializedName("Taxon") val taxon: Taxon
)

data class Taxon(
    @SerializedName("acceptedTaxon") val acceptedTaxon: AcceptedTaxon
)

data class AcceptedTaxon(
    @SerializedName("_id") val id: Int,
    @SerializedName("FullName") val fullName: String,
    @SerializedName("Vernacularname_DK") val vernacularNameDK: Vernacularname_DK?
)

data class Vernacularname_DK(
    val vernacularname_dk: String
)

data class ObservationImage(
    @SerializedName("name") val name: String,
    @SerializedName("createdAt") val createdAt: String
)

data class PrimaryUser(val profile: Profile)
data class Profile(
    val name: String,
    @SerializedName("Initialer") val initials: String,
    val facebook: String?
)




data class Forum(
    @SerializedName("_id") val id: Int,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("content") val content: String,
    @SerializedName("User") val user: PrimaryUser
)