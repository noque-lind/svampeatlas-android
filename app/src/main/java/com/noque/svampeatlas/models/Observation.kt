package com.noque.svampeatlas.models

import androidx.room.Database
import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import com.noque.svampeatlas.extensions.*
import com.noque.svampeatlas.extensions.Date
import com.noque.svampeatlas.services.RoomService
import com.noque.svampeatlas.utilities.SpeciesQueries
import com.noque.svampeatlas.view_models.NewObservationViewModel
import org.intellij.lang.annotations.Subst
import java.util.*

data class Observation(
    @SerializedName("_id") private val _id: Int = 0,
    @SerializedName("createdAt") private val _createdAt: String,
    @SerializedName("observationDate") private val _observationDate: String?,
    @SerializedName("ecologynote") private val _ecologyNote: String?,
    @SerializedName("note") private val _note: String?,
    @SerializedName("geom") private val _geom: Geom,
    @SerializedName("DeterminationView") private val _determinationView: DeterminationView?,
    @SerializedName("PrimaryDetermination") private val _primaryDetermination: PrimaryDeterminationView?,
    @SerializedName("Images") private val _observationImages: List<ObservationImage>?,
    @SerializedName("PrimaryUser") private val _primaryUser: PrimaryUser?,
    @SerializedName("Locality") private val _locality: Locality?,
    @SerializedName("GeoNames") private val _geoName: GeoName?,
    @SerializedName("Forum") private val _forum: MutableList<Forum>,
    @SerializedName("vegetationtype_id") private val vegetationTypeID: Int?,
    @SerializedName("substrate_id") private val substrateID: Int?,
    @SerializedName("accuracy") private val _accuracy: Int?,
    @SerializedName("Substrate") private val _substrate: Substrate?,
    @SerializedName("VegetationType") private val _vegetationType: VegetationType?,
    @SerializedName("associatedTaxa") private val _associatedTaxa: List<AssociatedTaxa>?

) {

    enum class ValidationStatus {
        APPROVED,
        VERIFYING,
        REJECTED,
        UNKNOWN
    }

    val id: Int
        get() {
            return _id
        }
    val createdAt: Date? get() {
        return Date(_createdAt)
    }

    val observationDate: Date? get() {
        return Date(_observationDate)
    }

    val coordinate: LatLng
        get() {
            return LatLng(_geom.coordinates.last(), _geom.coordinates.first())
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

    val locationName: String? get() {
        if (_geoName != null) {
            return "${_geoName.countryName}, ${_geoName.name}"
        } else {
            return _locality?.name
        }
    }

    val location: Location?
        get() {
            return Location(createdAt ?: Date(), coordinate, _accuracy?.toFloat() ?: -1F)
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

    val determination: Determination get() {
        return when {
            _primaryDetermination != null -> {
                Determination(
                    _primaryDetermination.taxon.acceptedTaxon.id,
                    _primaryDetermination.taxon.acceptedTaxon.fullName,
                    _primaryDetermination.taxon.acceptedTaxon.vernacularNameDK?.vernacularname_dk,
                    _primaryDetermination.confidence?.let { confidence -> DeterminationConfidence.values.first { it.databaseName == confidence } })
            }
            _determinationView != null -> {
                Determination(
                    _determinationView.taxonID,
                    _determinationView.fullName,
                    _determinationView.vernacularNameDK,
                    _determinationView.confidence?.let { confidence -> DeterminationConfidence.values.first { it.databaseName == confidence }  })
            }
            else -> throw InstantiationError()
        }
    }

    val images: List<Image>
        get() {
            return _observationImages?.map {
                Image(
                    it.id,
                    0,
                    "https://svampe.databasen.org/uploads/${it.name}.JPG",
                    null,
                    it.createdAt
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

    val hosts: List<Host> get() {
        return _associatedTaxa?.map { Host(it.id, it.dkName, it.name, null)
        } ?: listOf()
    }

    val vegetationType: VegetationType? get() {
        return when {
            _vegetationType != null -> {
                _vegetationType
            }
            vegetationTypeID != null -> {
                when (val result = RoomService.getVegetationTypewithIDNow(vegetationTypeID)) {
                    is Result.Success -> result.value
                    is Result.Error -> null
                }
            }
            else -> {
                null
            }
        }
    }

    val substrate: Substrate? get() {
        return when {
            _substrate != null -> {
                _substrate
            }
            substrateID != null -> {
                when (val result = RoomService.getSubstrateWithIDNow(substrateID)) {
                    is Result.Success -> result.value
                    is Result.Error -> null
                }
            }
            else -> null
        }
    }

    val locality: Locality? get() {
        return when {
             _geoName != null -> {
                Locality(_geoName.geonameId, _geoName.name, null, _geoName.lat.toDouble(), _geoName.lng.toDouble(), _geoName)
            }
            _locality != null -> {
                _locality
            }
            else -> null
        }
    }

    fun addComment(comment: Comment) {
        _forum.add(Forum(comment.id, comment.date?.toISO8601() ?: "", comment.content, PrimaryUser(Profile(comment.commenterName, comment.initials ?: "", comment.commenterProfileImageURL))))
    }

    fun isDeleteable(user: User): Boolean {
        if (user.isValidator) return true
        val createdAt = createdAt
        return user.name == observationBy && createdAt != null && createdAt.difDays() <= 2
    }

    fun isEditable(user: User): Boolean {
        if (user.isValidator) return true
        return user.name == observationBy
    }
}


data class Geom(val coordinates: List<Double>)

data class DeterminationView(
    @SerializedName("taxon_id") val taxonID: Int,
    @SerializedName("taxon_FullName") val fullName: String,
    @SerializedName("taxon_vernacularname_dk") val vernacularNameDK: String?,
    @SerializedName("redlistStatus") val redlistStatus: String?,
    @SerializedName("determination_validation") val determinationValidation: String?,
    @SerializedName("determination_score") val determinationScore: Int?,
    @SerializedName("confidence") val confidence: String?
)

data class PrimaryDeterminationView(
    @SerializedName("score") val score: Int?,
    @SerializedName("validation") val validation: String?,
    @SerializedName("Taxon") val taxon: Taxon,
    @SerializedName("confidence") val confidence: String?
)

enum class DeterminationConfidence(val databaseName: String) {
    CONFIDENT("sikker"),
    LIKELY("sandsynlig"),
    POSSIBLE("mulig");

    companion object {
        val values = values()
    }
}

data class Determination(
    val id: Int,
    val fullName: String,
    private val danishName: String?,
    val confidence: DeterminationConfidence?
) {
    val localizedName: String? get() {
        return if (Locale.getDefault().isDanish()) {
            return danishName?.capitalized()
        } else {
            null
        }
    }
}

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
    @SerializedName("_id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("createdAt") val createdAt: String
)

data class PrimaryUser(val profile: Profile)
data class Profile(
    val name: String,
    @SerializedName("Initialer") val initials: String,
    val facebook: String?
)

data class AssociatedTaxa(
    @SerializedName("_id")  val id: Int,
    @SerializedName("DKname")  val dkName: String?,
    @SerializedName("LatinName")  val name: String
)




data class Forum(
    @SerializedName("_id") val id: Int,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("content") val content: String,
    @SerializedName("User") val user: PrimaryUser
)
