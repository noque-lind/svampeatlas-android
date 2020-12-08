package com.noque.svampeatlas.utilities

import android.net.Uri
import android.util.Log
import com.android.volley.Request
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.extensions.toBounds
import com.noque.svampeatlas.extensions.toCircularPolygon
import com.noque.svampeatlas.extensions.toRectanglePolygon
import com.noque.svampeatlas.extensions.toSimpleString
import java.net.URLEncoder
import java.sql.Date
import java.time.LocalDateTime
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
        val builder = Uri.Builder()
        builder.scheme("https")
            .authority("svampe.databasen.org")
            .appendPath("api")

        when (request) {
            is APIType.Request.Mushrooms -> {
                val queries = request.queries.toMutableList()

                builder.appendPath("taxa")

                if (request.searchString != null) {
                    builder.appendQueryParameter("where", searchQuery(request.searchString))
                        .appendQueryParameter("nocount", "true")
                        .appendQueryParameter("order", "RankID ASC, probability DESC, FullName ASC")
                } else {
                    builder.appendQueryParameter("limit", request.limit.toString())
                        .appendQueryParameter("offset", request.offset.toString())
                        .appendQueryParameter("order", "FullName ASC")
                    queries.add(SpeciesQueries.Tag(16))
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
                            SpeciesQueries.RedlistData(),
                            SpeciesQueries.Statistics(),
                            SpeciesQueries.DanishNames()
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
                    val dateString = calendar.time.toSimpleString()
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
        }

        val url = builder.build().toString()
        Log.d("API", url)
        return url
    }

    private fun createPostURL(request: APIType.Post): String {
        val builder = Uri.Builder()
        builder.scheme("https")
            .authority("svampe.databasen.org")
            .appendPath("api")

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
                builder.appendQueryParameter("include", speciesIncludeQuery(listOf(SpeciesQueries.AcceptedTaxon(), SpeciesQueries.Images(false), SpeciesQueries.DanishNames(), SpeciesQueries.Attributes(null))))
            }

            is APIType.Post.OffensiveContentComment -> {
                builder.appendPath("observations")
                builder.appendPath("${request.observationID}")
                builder.appendPath("notifications")
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
            when (it) {
                is SpeciesQueries.Attributes -> {
                    if (it.presentInDenmark != null) {
                        string += "{\"model\":\"TaxonAttributes\",\"as\":\"attributes\",\"attributes\":[\"valideringsrapport\",\"PresentInDK\", \"diagnose\", \"beskrivelse\", \"forvekslingsmuligheder\", \"oekologi\", \"bogtekst_gyldendal_en\", \"vernacular_name_GB\", \"spiselighedsrapport\"],\"where\":\"{\\\"PresentInDK\\\":${it.presentInDenmark}}\"}"
                    } else {
                        string += "{\"model\":\"TaxonAttributes\",\"as\":\"attributes\",\"attributes\":[\"valideringsrapport\",\"PresentInDK\", \"diagnose\", \"beskrivelse\", \"forvekslingsmuligheder\", \"oekologi\", \"bogtekst_gyldendal_en\", \"vernacular_name_GB\", \"spiselighedsrapport\"]}"
                    }
                }
                is SpeciesQueries.Images -> {
                    string += "{\"model\":\"TaxonImages\",\"as\":\"Images\",\"required\":${it.required}}"
                }

                is SpeciesQueries.DanishNames -> {
                    string += "{\"model\":\"TaxonDKnames\",\"as\":\"Vernacularname_DK\", \"attributes\":[\"vernacularname_dk\", \"source\"]}"
                }

                is SpeciesQueries.Statistics -> {
                    string += "{\"model\":\"TaxonStatistics\",\"as\":\"Statistics\", \"attributes\":[\"accepted_count\", \"last_accepted_record\", \"first_accepted_record\"]}"
                }
                is SpeciesQueries.RedlistData -> {
                    string += "{\"model\":\"TaxonRedListData\",\"as\":\"redlistdata\",\"required\":false,\"attributes\":[\"status\"],\"where\":\"{\\\"year\\\":2019}\"}"
                }

                is SpeciesQueries.Tag -> {
                    string += "{\"model\":\"TaxonomyTagView\",\"as\":\"tags0\",\"where\":\"{\\\"tag_id\\\":${it.id}}\"}"
                }

                is SpeciesQueries.AcceptedTaxon -> {
                    string += "{\"model\":\"Taxon\",\"as\":\"acceptedTaxon\"}"
                }
            }

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

        return "{\"RankID\":{\"gt\":4999},\"\$or\":[{\"FullName\":{\"like\":\"%${fullSearchTerm}%\"}},{\"\$Vernacularname_DK.vernacularname_dk\$\":{\"like\":\"%${fullSearchTerm}%\"}},{\"FullName\":{\"like\":\"${genus}%\"},\"TaxonName\":{\"like\":\"${taxonName}%\"}}]}"
    }

    private fun observationIncludeQuery(observationQueries: List<ObservationQueries>): String {
        var string = "["

        observationQueries.forEach {
            when (it) {
                is ObservationQueries.Images -> {
                    string += "\"{\\\"model\\\":\\\"ObservationImage\\\",\\\"as\\\":\\\"Images\\\",\\\"where\\\":{},\\\"required\\\":false}\""
                }
                is ObservationQueries.Comments -> {
                    string += "\"{\\\"model\\\":\\\"ObservationForum\\\",\\\"as\\\":\\\"Forum\\\",\\\"where\\\":{},\\\"required\\\":false}\""
                }
                is ObservationQueries.DeterminationView -> {
                    if (it.taxonID != null) {
                        string += "\"{\\\"model\\\":\\\"DeterminationView\\\",\\\"as\\\":\\\"DeterminationView\\\",\\\"attributes\\\":[\\\"taxon_id\\\",\\\"recorded_as_id\\\",\\\"taxon_FullName\\\",\\\"taxon_vernacularname_dk\\\",\\\"determination_validation\\\",\\\"recorded_as_FullName\\\",\\\"determination_user_id\\\",\\\"determination_score\\\",\\\"determination_validator_id\\\",\\\"determination_species_hypothesis\\\"],\\\"where\\\":{\\\"Taxon_id\\\":${it.taxonID}}}\""
                    } else {
                        string += "\"{\\\"model\\\":\\\"DeterminationView\\\",\\\"as\\\":\\\"DeterminationView\\\",\\\"attributes\\\":[\\\"taxon_id\\\",\\\"recorded_as_id\\\",\\\"taxon_FullName\\\",\\\"taxon_vernacularname_dk\\\",\\\"determination_validation\\\",\\\"recorded_as_FullName\\\",\\\"determination_user_id\\\",\\\"determination_score\\\",\\\"determination_validator_id\\\",\\\"determination_species_hypothesis\\\"]}\""
                    }
                }
                is ObservationQueries.User -> {
                    if (it.responseFilteredByUserID != null) {
                        string += "\"{\\\"model\\\":\\\"User\\\",\\\"as\\\":\\\"PrimaryUser\\\",\\\"required\\\":true,\\\"where\\\":{\\\"_id\\\":${it.responseFilteredByUserID}}}\""
                    } else {
                        string += "\"{\\\"model\\\":\\\"User\\\",\\\"as\\\":\\\"PrimaryUser\\\",\\\"required\\\":true}\""
                    }
                }

                is ObservationQueries.Locality -> {
                    string += "\"{\\\"model\\\":\\\"Locality\\\",\\\"as\\\":\\\"Locality\\\",\\\"attributes\\\":[\\\"_id\\\",\\\"name\\\"]}\""
                }

                is ObservationQueries.GeomNames -> {
                    string += "\"{\\\"model\\\":\\\"GeoNames\\\",\\\"as\\\":\\\"GeoNames\\\",\\\"where\\\":{},\\\"required\\\":false}\""
                }
            }

            string += ","
        }

        string = string.dropLast(1)
        string += "]"
        return string
    }

}

sealed class SpeciesQueries : APIType() {
    class Attributes(val presentInDenmark: Boolean?) : SpeciesQueries()
    class Images(val required: Boolean) : SpeciesQueries()
    class DanishNames() : SpeciesQueries()
    class Statistics() : SpeciesQueries()
    class RedlistData() : SpeciesQueries()
    class Tag(val id: Int): SpeciesQueries()
    class AcceptedTaxon: SpeciesQueries()
}

sealed class ObservationQueries : APIType() {
    class Images() : ObservationQueries()
    class Comments() : ObservationQueries()
    class DeterminationView(val taxonID: Int?) : ObservationQueries()
    class User(val responseFilteredByUserID: Int?) : ObservationQueries()
    class Locality : ObservationQueries()
    class GeomNames : ObservationQueries()
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

sealed class APIType {
    sealed class Request : APIType() {
        class Mushrooms(
            val searchString: String?,
            val queries: List<SpeciesQueries>,
            val offset: Int,
            val limit: Int
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
    }

    sealed class Post : APIType() {
        class Observation() : Post()
        class Image(val observationID: Int) : Post()
        class Login() : Post()
        class Comment(val taxonID: Int) : Post()
        class ImagePrediction: Post()
        class OffensiveContentComment(val observationID: Int): Post()
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
