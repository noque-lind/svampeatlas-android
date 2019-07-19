package com.noque.svampeatlas.Model

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName

data class Observation(
    @SerializedName("_id") private val _id: Int = 0,
    @SerializedName("observationDate") private val _observationDate: String?,
    @SerializedName("ecologynote") private val _ecologyNote: String?,
    @SerializedName("note") private val _note: String?,
    @SerializedName("geom") private val _geom: Geom,
    @SerializedName("DeterminationView") private val _determinationView: DeterminationView?,
    @SerializedName("Images") private val _observationImages: List<ObservationImage>,
    @SerializedName("PrimaryUser") private val _primaryUser: PrimaryUser?,
    @SerializedName("Locality") private val _locality: Locality?,
    @SerializedName("Forum") private val _forum: List<Forum>,
    @SerializedName("vegetationtype_id") private val _vegetationTypeID: Int?,
    @SerializedName("substrate_id") private val substrateID: Int?
) {

    val id: Int get() {return _id}
    val coordinate: LatLng get() {return LatLng(_geom.coordinates.last(), _geom.coordinates.first())}
    val date: String? get() {return _observationDate}
    val observationBy: String? get() {return _primaryUser?.profile?.name}
    val note: String? get() {return _note}
    val ecologyNote: String? get() {return _ecologyNote}
    val location: String? get() {return _locality?.name}
    val images: List<Image>? get() {return _observationImages.map { Image(null, "https://svampe.databasen.org/uploads/${it.name}.JPG", null)}}
    val comments: List<Comment>? get() {return _forum.mapNotNull {
        if (it.id != null && it.createdAt != null && it.content != null && it.user?.name != null && it.user?.initials != null && it.user?.facebook != null) {
            Comment(
                it.id,
                it.createdAt,
                it.content,
                it.user.name,
                it.user.initials,
                it.user.facebook)
        } else {
            null
        }
        }}
}


data class Geom(val coordinates: List<Double>)
data class DeterminationView(
    @SerializedName("taxon_id") val taxonID: Int?,
    @SerializedName("taxon_FullName") val fullName: String?,
    @SerializedName("taxon_vernacularname_dk") val vernacularNameDK: String?,
    @SerializedName("redlistStatus") val redlistStatus: String?,
    @SerializedName("determination_validation") val determinationValidation: String?
)

data class ObservationImage(
    @SerializedName("name") val name: String,
    @SerializedName("createdAt") val createdAt: String
)

data class PrimaryUser(val profile: Profile?)
data class Profile(
    val name: String?,
    @SerializedName("Initialer") val initials: String?,
    val facebook: String?
                   )

data class Locality(
    val name: String?,
    @SerializedName("kommune") val municipality: String?
)

data class Forum(
    @SerializedName("_id") val id: Int?,
    @SerializedName("createdAt") val createdAt: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("User") val user: Profile?
)