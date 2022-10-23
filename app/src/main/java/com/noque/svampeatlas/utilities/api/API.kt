package com.noque.svampeatlas.utilities.api

import android.net.Uri
import android.util.Log
import com.android.volley.Request
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.extensions.*
import java.util.*

data class API(val apiType: APIType) {

    enum class Radius(val radius: Int) {
        SMALLEST(800),
        SMALLER(1000),
        SMALL(1200),
        MEDIUM(1400),
        LARGE(1600),
        LARGER(1800),
        LARGEST(2000),
        HUGE(2500),
        HUGER(5000),
        HUGEST(1000),
        COUNTRY(0)
    }

    fun url(): String {
        return when (apiType) {
            is APIType.Request -> createGetURL(apiType)
            is APIType.Post -> createPostURL(apiType)
            is APIType.Put -> createPutURL(apiType)
            is APIType.Delete -> createDeleteURL(apiType)
            else -> ""
        }
    }

    fun volleyMethod(): Int {
        return when (apiType) {
            is APIType.Request -> Request.Method.GET
            is APIType.Post -> Request.Method.POST
            is APIType.Put -> Request.Method.PUT
            is APIType.Delete -> Request.Method.DELETE
            else -> Request.Method.GET
        }
    }

    private fun createGetURL(request: APIType.Request): String {


        val builder = Uri.Builder().scheme("https")
        when (request) {
            is APIType.Request.ImagePredictionGetResults -> builder.authority("fungi.piva-ai.com")
            else ->  builder
                .authority("svampe.databasen.org")
                .appendPath("api")
        }

        when (request) {
            is APIType.Request.Mushrooms -> {
                val queries = request.queries.toMutableList()
                queries.add(SpeciesQueries.AcceptedTaxon)
                builder.appendPath("taxa")

                if (request.searchString != null) {
                    builder.appendQueryParameter("where", searchQuery(request.searchString))
                        .appendQueryParameter("nocount", "true")
                        .appendQueryParameter("order", "RankID ASC, probability DESC, FullName ASC")
                } else {
                    if (request.limit != null) {
                        builder.appendQueryParameter("limit", request.limit.toString())
                        queries.add(SpeciesQueries.Tag(16))
                    }
                    builder.appendQueryParameter("offset", request.offset.toString())
                        .appendQueryParameter("order", "FullName ASC")
                }
                builder.appendQueryParameter("include", speciesIncludeQuery(queries))
            }

            is APIType.Request.Mushroom -> {
                builder.appendPath("taxa")
                builder.appendQueryParameter("where", "{\"_id\":${request.id}}")
                builder.appendQueryParameter(
                    "include",
                    speciesIncludeQuery(
                        listOf(
                            SpeciesQueries.Images(false),
                            SpeciesQueries.Attributes(null),
                            SpeciesQueries.RedlistData,
                            SpeciesQueries.Statistics,
                            SpeciesQueries.DanishNames,
                            SpeciesQueries.AcceptedTaxon
                        )
                    )
                )
            }

            is APIType.Request.Observation -> {
                builder.appendPath("observations")
                builder.appendQueryParameter(
                    "_order",
                    "[[\"observationDate\",\"DESC\",\"ASC\"],[\"_id\",\"DESC\"]]"
                )

                builder.appendQueryParameter(
                    "include",
                    observationIncludeQuery(request.observationQueries)
                )

                request.ageInYear?.let {
                    val calendar = Calendar.getInstance()
                    calendar.add(Calendar.MONTH, -it * 12)
                    val dateString = calendar.time.toDatabaseName()
                    builder.appendQueryParameter("where", "{\"observationDate\":{\"\$gte\":\"$dateString\"}}")
                }

                request.limit?.let {
                    builder.appendQueryParameter("limit", it.toString())
                }

                request.offset?.let {
                    builder.appendQueryParameter("offset", it.toString())
                }

                request.geometry?.let {
                    builder.appendQueryParameter("geometry", it.toGeoJson())
                }
            }

            is APIType.Request.Locality -> {
                builder.appendPath("localities")
                builder.appendQueryParameter("where", request.geometry.toBetween())
            }

            is APIType.Request.GeomNames -> {
                builder.appendPath("geonames")
                builder.appendPath("findnearby")
                builder.appendQueryParameter("lat", "${request.coordinate.latitude}")
                builder.appendQueryParameter("lng", "${request.coordinate.longitude}")
            }

            is APIType.Request.Substrate -> {
                builder.appendPath("substrate")
            }

            is APIType.Request.VegetationType -> {
                builder.appendPath("vegetationTypes")
            }

            is APIType.Request.Host -> {
                builder.appendPath("planttaxa")
                builder.encodedQuery("order=probability+DESC")
                builder.appendQueryParameter("limit", "30")
                request.searchString?.let {
                    builder.appendQueryParameter(
                        "where",
                        "{\"\$or\":[{\"DKname\":{\"like\":\"${it}%\"}},{\"LatinName\":{\"like\":\"${it}%\"}}]}"
                    )
                }
            }

            is APIType.Request.User -> {
                builder.appendPath("users")
                builder.appendPath("me")
            }

            is APIType.Request.UserNotificationCount -> {
                builder.appendPath("users")
                builder.appendPath("me")
                builder.appendPath("feed")
                builder.appendPath("count")
            }

            is APIType.Request.UserNotifications -> {
                builder.appendPath("users")
                builder.appendPath("me")
                builder.appendPath("feed")
                builder.appendQueryParameter("limit", request.limit.toString())
                builder.appendQueryParameter("offset", request.offset.toString())
            }

            is APIType.Request.SingleObservation -> {
                builder.appendPath("observations")
                builder.appendPath("${request.id}")
            }

            is APIType.Request.ObservationCountForUser -> {
                builder.appendPath("users")
                builder.appendPath(request.userId.toString())
                builder.appendPath("observations")
                builder.appendPath("count")
            }
            is APIType.Request.ImagePredictionGetResults -> builder.appendPath("get_results").appendPath(request.id)
        }

        val url = builder.build().toString()
        Log.d("API", url)
        return url
    }

    private fun createPostURL(request: APIType.Post): String {
        val builder = Uri.Builder()
        builder.scheme("https")

        when (request) {
           is APIType.Post.ImagePredictionAddPhoto, is APIType.Post.ImagePredictionAddMetaData -> {
               builder.authority("fungi.piva-ai.com")
           }
            else -> {
                builder.authority("svampe.databasen.org")
                builder.appendPath("api")
            }
        }

        when (request) {
            is APIType.Post.Observation -> {
                builder.appendPath("observations")
            }

            is APIType.Post.Image -> {
                builder.appendPath("observations")
                builder.appendPath(request.observationID.toString())
                builder.appendPath("images")
            }

            is APIType.Post.Login -> {
                builder.path("auth/local")
            }

            is APIType.Post.Comment -> {
                builder.appendPath("observations")
                builder.appendPath(request.taxonID.toString())
                builder.appendPath("forum")
            }

            is APIType.Post.ImagePrediction -> {
                builder.appendPath("imagevision")
                builder.appendQueryParameter("include", speciesIncludeQuery(listOf(
                    SpeciesQueries.AcceptedTaxon,
                    SpeciesQueries.Images(false),
                    SpeciesQueries.DanishNames,
                    SpeciesQueries.Attributes(null)
                )))
            }

            is APIType.Post.OffensiveContentComment -> {
                builder.appendPath("observations")
                builder.appendPath("${request.observationID}")
                builder.appendPath("notifications")
            }
            is APIType.Post.ImagePredictionAddPhoto -> {
                builder.appendPath("add_photo")
                if (request.id != null) builder.appendPath(request.id)
            }
            is APIType.Post.ImagePredictionAddMetaData -> {
    builder.appendPath("add_metadata").appendPath(request.id)
            }
        }

        return builder.build().toString()
    }

    private fun createPutURL(request: APIType.Put): String {
        val builder = Uri.Builder()
        builder.scheme("https")
            .authority("svampe.databasen.org")
            .appendPath("api")

        when (request) {
            is APIType.Put.NotificationLastRead -> {
                builder.appendPath("users")
                builder.appendPath("me")
                builder.appendPath("feed")
                builder.appendPath(request.notificationID.toString())
                builder.appendPath("lastread")
            }
            is APIType.Put.Observation -> {
                builder.appendPath("observations")
                builder.appendPath(request.id.toString())
            }
        }

        return builder.build().toString()
    }

    private fun createDeleteURL(request: APIType.Delete): String {
        val builder = Uri.Builder()
        builder.scheme("https")
            .authority("svampe.databasen.org")
            .appendPath("api")

        when (request) {
            is APIType.Delete.Image -> {
                builder.appendPath("observationimages")
                builder.appendPath(request.id.toString())
            }
            is APIType.Delete.Observation -> {
                builder.appendPath("observations")
                builder.appendPath(request.id.toString())
            }
        }

        return builder.build().toString()
    }

    private fun speciesIncludeQuery(queries: List<SpeciesQueries>): String {
        var string = "["

        queries.forEach {
            string += it.queryString()
            string += ","
        }

        if (queries.isNotEmpty()) string = string.dropLast(1)

        string += "]"
        return string
    }

    private fun searchQuery(searchString: String): String {
        var genus = ""
        var fullSearchTerm = ""
        var taxonName = ""

        searchString.split(" ").toTypedArray().forEach {
            if (it != "") {
                if (fullSearchTerm == "") {
                    fullSearchTerm = it
                    genus = it
                } else {
                    fullSearchTerm += " ${it}"

                    if (taxonName == "") {
                        taxonName = it
                    } else {
                        taxonName += " ${it}"
                    }
                }
            }
        }

        return when (Locale.getDefault().appLanguage()) {
            AppLanguage.Danish -> "{\"RankID\":{\"gt\":4999},\"\$or\":[{\"FullName\":{\"like\":\"%${fullSearchTerm}%\"}},{\"\$Vernacularname_DK.vernacularname_dk\$\":{\"like\":\"%${fullSearchTerm}%\"}},{\"FullName\":{\"like\":\"${genus}%\"},\"TaxonName\":{\"like\":\"${taxonName}%\"}}]}"
            AppLanguage.English -> "{\"RankID\":{\"gt\":4999},\"\$or\":[{\"FullName\":{\"like\":\"%${fullSearchTerm}%\"}},{\"\$attributes.vernacular_name_GB\$\":{\"like\":\"%${fullSearchTerm}%\"}},{\"FullName\":{\"like\":\"${genus}%\"},\"TaxonName\":{\"like\":\"${taxonName}%\"}}]}"
            AppLanguage.Czech -> "{\"RankID\":{\"gt\":4999},\"\$or\":[{\"FullName\":{\"like\":\"%${fullSearchTerm}%\"}},{\"\$attributes.vernacular_name_CZ\$\":{\"like\":\"%${fullSearchTerm}%\"}},{\"FullName\":{\"like\":\"${genus}%\"},\"TaxonName\":{\"like\":\"${taxonName}%\"}}]}"
        }
    }

    private fun observationIncludeQuery(observationQueries: List<ObservationQueries>): String {
        var string = "["

        observationQueries.forEach {
            string += it.queryString()
            string += ","
        }

        string = string.dropLast(1)
        string += "]"
        return string
    }

}

sealed class SpeciesQueries: APIType() {
    class Attributes(val presentInDenmark: Boolean?) : SpeciesQueries()
    class Images(val required: Boolean) : SpeciesQueries()
    class Tag(val id: Int): SpeciesQueries()
    object DanishNames : SpeciesQueries()
    object Statistics : SpeciesQueries()
    object RedlistData : SpeciesQueries()
    object AcceptedTaxon : SpeciesQueries()

    fun queryString(): String {
        return when (this) {
            is Attributes -> {
                if (this.presentInDenmark != null) {
                    "{\"model\":\"TaxonAttributes\",\"as\":\"attributes\",\"attributes\":[\"valideringsrapport\",\"PresentInDK\", \"diagnose\", \"beskrivelse\", \"forvekslingsmuligheder\", \"oekologi\", \"bogtekst_gyldendal_en\", \"vernacular_name_GB\", \"vernacular_name_CZ\", \"spiselighedsrapport\"],\"where\":\"{\\\"PresentInDK\\\":${this.presentInDenmark}}\"}"
                } else {
                    "{\"model\":\"TaxonAttributes\",\"as\":\"attributes\",\"attributes\":[\"valideringsrapport\",\"PresentInDK\", \"diagnose\", \"beskrivelse\", \"forvekslingsmuligheder\", \"oekologi\", \"bogtekst_gyldendal_en\", \"vernacular_name_GB\", \"vernacular_name_CZ\", \"spiselighedsrapport\"]}"
                }
            }
            is Images -> "{\"model\":\"TaxonImages\",\"as\":\"Images\",\"required\":${this.required}}"
            is DanishNames -> "{\"model\":\"TaxonDKnames\",\"as\":\"Vernacularname_DK\", \"attributes\":[\"vernacularname_dk\", \"source\"]}"
            is Statistics -> "{\"model\":\"TaxonStatistics\",\"as\":\"Statistics\", \"attributes\":[\"accepted_count\", \"last_accepted_record\", \"first_accepted_record\"]}"
            is RedlistData -> "{\"model\":\"TaxonRedListData\",\"as\":\"redlistdata\",\"required\":false,\"attributes\":[\"status\"],\"where\":\"{\\\"year\\\":2019}\"}"
            is Tag -> "{\"model\":\"TaxonomyTagView\",\"as\":\"tags0\",\"where\":\"{\\\"tag_id\\\":${this.id}}\"}"
            is AcceptedTaxon -> "{\"model\":\"Taxon\",\"as\":\"acceptedTaxon\"}"
        }
    }
}

sealed class ObservationQueries: APIType() {
    object Images : ObservationQueries()
    object Comments : ObservationQueries()
    object Locality : ObservationQueries()
    object GeomNames : ObservationQueries()
    class DeterminationView(val taxonID: Int?) : ObservationQueries()
    class User(val responseFilteredByUserID: Int?) : ObservationQueries()

    fun queryString(): String {
       return when (this) {
            is Images -> "\"{\\\"model\\\":\\\"ObservationImage\\\",\\\"as\\\":\\\"Images\\\",\\\"where\\\":{},\\\"required\\\":false}\""
            is Comments -> "\"{\\\"model\\\":\\\"ObservationForum\\\",\\\"as\\\":\\\"Forum\\\",\\\"where\\\":{},\\\"required\\\":false}\""
            is DeterminationView -> {
                if (this.taxonID != null) {
                    "\"{\\\"model\\\":\\\"DeterminationView\\\",\\\"as\\\":\\\"DeterminationView\\\",\\\"attributes\\\":[\\\"taxon_id\\\",\\\"recorded_as_id\\\",\\\"taxon_FullName\\\",\\\"taxon_vernacularname_dk\\\",\\\"determination_validation\\\",\\\"recorded_as_FullName\\\",\\\"determination_user_id\\\",\\\"determination_score\\\",\\\"determination_validator_id\\\",\\\"determination_species_hypothesis\\\"],\\\"where\\\":{\\\"Taxon_id\\\":${this.taxonID}}}\""
                } else {
                    "\"{\\\"model\\\":\\\"DeterminationView\\\",\\\"as\\\":\\\"DeterminationView\\\",\\\"attributes\\\":[\\\"taxon_id\\\",\\\"recorded_as_id\\\",\\\"taxon_FullName\\\",\\\"taxon_vernacularname_dk\\\",\\\"determination_validation\\\",\\\"recorded_as_FullName\\\",\\\"determination_user_id\\\",\\\"determination_score\\\",\\\"determination_validator_id\\\",\\\"determination_species_hypothesis\\\"]}\""
                }
            }
            is User -> {
                if (this.responseFilteredByUserID != null) {
                    "\"{\\\"model\\\":\\\"User\\\",\\\"as\\\":\\\"PrimaryUser\\\",\\\"required\\\":true,\\\"where\\\":{\\\"_id\\\":${this.responseFilteredByUserID}}}\""
                } else {
                     "\"{\\\"model\\\":\\\"User\\\",\\\"as\\\":\\\"PrimaryUser\\\",\\\"required\\\":true}\""
                }
            }

            is Locality -> "\"{\\\"model\\\":\\\"Locality\\\",\\\"as\\\":\\\"Locality\\\",\\\"attributes\\\":[\\\"_id\\\",\\\"name\\\"]}\""
            is GeomNames -> "\"{\\\"model\\\":\\\"GeoNames\\\",\\\"as\\\":\\\"GeoNames\\\",\\\"where\\\":{},\\\"required\\\":false}\""
        }
    }
}

data class Geometry(
    val coordinate: LatLng,
    val radius: Int,
    val type: Type
) {

    enum class Type {
        CIRCLE, RECTANGLE
    }

    fun toGeoJson(): String {
        var string =
            "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[["

        when (type) {
            Type.RECTANGLE -> {
                coordinate.toRectanglePolygon(radius)
                    .forEach { string += "[${it.longitude},${it.latitude}]," }
            }
            Type.CIRCLE -> {
                coordinate.toCircularPolygon(radius).forEach { string += "[${it.longitude},${it.latitude}]," }
            }
        }

        string = string.dropLast(1)
        string += "]]}}"
        Log.d("API", string)
        return string
    }


    fun toBetween(): String {
        val bounds = this.coordinate.toBounds(this.radius)
        val northeast = bounds.northeast
        val southwest = bounds.southwest
        return "{\"decimalLongitude\":{\"\$between\":[${southwest.longitude},${northeast.longitude}]},\"decimalLatitude\":{\"\$between\":[${southwest.latitude},${northeast.latitude}]}}"
    }
}

sealed class APIType() {

    sealed class Request : APIType() {
        class Mushrooms(
            val searchString: String?,
            val queries: List<SpeciesQueries>,
            val offset: Int,
            val limit: Int?
        ) : Request()

        class Mushroom(val id: Int) : Request()

        class Observation(
            val geometry: Geometry?,
            val observationQueries: List<ObservationQueries>,
            val ageInYear: Int?,
            val limit: Int?,
            val offset: Int?
        ) : Request()

        class SingleObservation(val id: Int) : Request()
        class Locality(val geometry: Geometry) : Request()
        class GeomNames(val coordinate: LatLng): Request()
        class Substrate() : Request()
        class VegetationType() : Request()
        class Host(val searchString: String?) : Request()
        class User : Request()
        class UserNotificationCount() : Request()
        class UserNotifications(val limit: Int, val offset: Int) : Request()
        class ObservationCountForUser(val userId: Int) : Request()
        class ImagePredictionGetResults(val id: String): Request()
    }

    sealed class Post : APIType() {
        object Observation : Post()
        class Image(val observationID: Int) : Post()
        object Login : Post()
        class Comment(val taxonID: Int) : Post()
        object ImagePrediction : Post()
        class OffensiveContentComment(val observationID: Int): Post()
        class ImagePredictionAddPhoto(val id: String?) : Post()
        class ImagePredictionAddMetaData(val id: String): Post()
    }

    sealed class Put: APIType() {
        class NotificationLastRead(val notificationID: Int): Put()
        class Observation(val id: Int): Put()
    }

    sealed class Delete: APIType() {
        class Image(val id: Int): Delete()
        class Observation(val id: Int): Delete()
    }
}
