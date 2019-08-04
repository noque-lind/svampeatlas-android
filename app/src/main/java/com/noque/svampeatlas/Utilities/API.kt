package com.noque.svampeatlas.Utilities

import android.net.Uri
import android.util.Log
import com.android.volley.Request
import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.Extensions.toBounds
import com.noque.svampeatlas.Extensions.toRectanglePolygon

data class API(val apiType: APIType) {

    private val BASE_URL_API = "https://" + "svampe.databasen.org" + "/api/"

    fun url(): String {
        when (apiType) {
            is APIType.Request -> return createRequestURL(apiType)
            else -> return ""
        }
    }

    fun volleyMethod(): Int {
        when (apiType) {
            is APIType.Request -> return Request.Method.GET
            else -> return Request.Method.GET
        }
    }

    private fun createRequestURL(request: APIType.Request): String {
        val builder = Uri.Builder()
        builder.scheme("https")
            .authority("svampe.databasen.org")
            .appendPath("api")

        when (request) {
            is APIType.Request.Mushroom -> {
                builder.appendPath("taxa")

                if (request.searchString != null) {
                    builder.appendQueryParameter("where", searchQuery(request.searchString))
                        .appendQueryParameter("nocount", "true")
                } else {
                    builder.appendQueryParameter("limit", request.limit.toString())
                        .appendQueryParameter("offset", request.offset.toString())
                }

                .appendQueryParameter("_order", "[[\"FullName\"]]")
                    .appendQueryParameter("acceptedTaxaOnly", "true")
                .appendQueryParameter("include", speciesIncludeQuery(request.requirePictures))


            }

            is APIType.Request.Observation -> {
                builder.appendPath("observations")
                    builder.appendQueryParameter("include", observationIncludeQuery(request.observationQueries))

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

            is APIType.Request.Substrate -> {
                builder.appendPath("substrate")
            }

            is APIType.Request.VegetationType -> {
                builder.appendPath("vegetationTypes")
            }
        }

        val url = builder.build().toString()
        Log.d("API", url)
        return url
    }

    private fun speciesIncludeQuery(imagesRequired: Boolean): String {
        return "[{\"model\":\"TaxonRedListData\",\"as\":\"redlistdata\",\"required\":false,\"attributes\":[\"status\"],\"where\":\"{\\\"year\\\":2009}\"},{\"model\":\"Taxon\",\"as\":\"acceptedTaxon\"},{\"model\":\"TaxonAttributes\",\"as\":\"attributes\",\"attributes\":[\"PresentInDK\", \"forvekslingsmuligheder\", \"oekologi\", \"diagnose\"],\"where\":\"{\\\"PresentInDK\\\":true}\"},{\"model\":\"TaxonDKnames\",\"as\":\"Vernacularname_DK\",\"required\":false},{\"model\":\"TaxonStatistics\",\"as\":\"Statistics\",\"required\":false},{\"model\":\"TaxonImages\",\"as\":\"images\",\"required\":${imagesRequired}}]"

//        return "%5B%7B%22model%22%3A%22TaxonRedListData%22%2C%22as%22%3A%22redlistdata%22%2C%22required%22%3Afalse%2C%22attributes%22%3A%5B%22status%22%5D%2C%22where%22%3A%22%7B%5C%22year%5C%22%3A2009%7D%22%7D%2C%7B%22model%22%3A%22Taxon%22%2C%22as%22%3A%22acceptedTaxon%22%7D%2C%7B%22model%22%3A%22TaxonAttributes%22%2C%22as%22%3A%22attributes%22%2C%22attributes%22%3A%5B%22PresentInDK%22%2C%20%22forvekslingsmuligheder%22%2C%20%22oekologi%22%2C%20%22diagnose%22%5D%2C%22where%22%3A%22%7B%5C%22PresentInDK%5C%22%3Atrue%7D%22%7D%2C%7B%22model%22%3A%22TaxonDKnames%22%2C%22as%22%3A%22Vernacularname_DK%22%2C%22required%22%3Afalse%7D%2C%7B%22model%22%3A%22TaxonStatistics%22%2C%22as%22%3A%22Statistics%22%2C%22required%22%3Afalse%7D%2C%7B%22model%22%3A%22TaxonImages%22%2C%22as%22%3A%22images%22%2C%22required%22%3A${imagesRequired}%7D%5D"
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
                    fullSearchTerm += "+${it}"

                    if (taxonName == "") {
                        taxonName = it
                    } else {
                        taxonName += "+${it}"
                    }
                }
            }
        }

        Log.d("Dataservice", "Search terms: FullSearchterm: ${fullSearchTerm}, genus: ${genus}, taxonName: ${taxonName}")

        return "{\"\$or\":[{\"FullName\":{\"like\":\"%${fullSearchTerm}%\"}},{\"\$Vernacularname_DK.vernacularname_dk\$\":{\"like\":\"%${fullSearchTerm}%\"}},{\"FullName\":{\"like\":\"${genus}%\"},\"TaxonName\":{\"like\":\"${taxonName}%\"}}]}"
        }

    private fun observationIncludeQuery(observationQueries: List<ObservationQueries>): String {
        var string = "["

        observationQueries.forEach {
            when (it) {
                is ObservationQueries.Images -> {string += "\"{\\\"model\\\":\\\"ObservationImage\\\",\\\"as\\\":\\\"Images\\\",\\\"where\\\":{},\\\"required\\\":false}\""}
                is ObservationQueries.Comments -> {string += "\"{\\\"model\\\":\\\"ObservationForum\\\",\\\"as\\\":\\\"Forum\\\",\\\"where\\\":{},\\\"required\\\":false}\""}
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

                is ObservationQueries.Locality -> {string += "\"{\\\"model\\\":\\\"Locality\\\",\\\"as\\\":\\\"Locality\\\",\\\"attributes\\\":[\\\"_id\\\",\\\"name\\\"]}\""}
            }

            string += ","
        }

        string = string.dropLast(1)
        string += "]"
return string
    }

}

sealed class ObservationQueries: APIType() {
    class Images(): ObservationQueries()
    class Comments(): ObservationQueries()
    class DeterminationView(val taxonID: Int?):ObservationQueries()
    class User(val responseFilteredByUserID: Int?):ObservationQueries()
    class Locality:ObservationQueries()
}

data class Geometry(val coordinate: LatLng,
                    val radius: Double,
                    val type: Type) {

    enum class Type {
        CIRCLE, RECTANGLE
    }

    fun toGeoJson(): String {
        var string = "{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[["

        when (type) {
            Type.RECTANGLE -> {coordinate.toRectanglePolygon(radius).forEach { string += "[${it.longitude},${it.latitude}]," }}
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
     sealed class Request: APIType() {
       class Mushroom(val offset: Int, val limit: Int, val searchString: String?, val requirePictures: Boolean): Request()
         class Observation(val geometry: Geometry?, val observationQueries: List<ObservationQueries>, val ageInYear: Int?, val limit: Int?, val offset: Int?): Request()
         class Locality(val geometry: Geometry): Request()
         class Substrate(): Request()
         class VegetationType(): Request()
    }

}
