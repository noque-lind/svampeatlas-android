package com.noque.svampeatlas.Utilities

import com.android.volley.Request

data class API(val apiType: APIType) {

    private val BASE_URL_API = "https://" + "svampe.databasen.org" + "/api/"

    fun url(): String {
        when (apiType) {
            is APIType.Request -> return returnRequestURL(apiType)
            else -> return ""
        }
    }

    fun volleyMethod(): Int {
        when (apiType) {
            is APIType.Request -> return Request.Method.GET
            else -> return Request.Method.GET
        }
    }

    private fun returnRequestURL(request: APIType.Request): String {
        when (request) {
            is APIType.Request.Mushroom -> return BASE_URL_API + "taxa?_order=%5B%5B%22FullName%22%5D%5D&acceptedTaxaOnly=true" + speciesIncludeQuery(request.requirePictures) + "&limit=${request.limit}" + "&offset=${request.offset}"
            else -> return ""
        }
    }

    private fun speciesIncludeQuery(imagesRequired: Boolean): String {
        return "&include=%5B%7B%22model%22%3A%22TaxonRedListData%22%2C%22as%22%3A%22redlistdata%22%2C%22required%22%3Afalse%2C%22attributes%22%3A%5B%22status%22%5D%2C%22where%22%3A%22%7B%5C%22year%5C%22%3A2009%7D%22%7D%2C%7B%22model%22%3A%22Taxon%22%2C%22as%22%3A%22acceptedTaxon%22%7D%2C%7B%22model%22%3A%22TaxonAttributes%22%2C%22as%22%3A%22attributes%22%2C%22attributes%22%3A%5B%22PresentInDK%22%2C%20%22forvekslingsmuligheder%22%2C%20%22oekologi%22%2C%20%22diagnose%22%5D%2C%22where%22%3A%22%7B%5C%22PresentInDK%5C%22%3Atrue%7D%22%7D%2C%7B%22model%22%3A%22TaxonDKnames%22%2C%22as%22%3A%22Vernacularname_DK%22%2C%22required%22%3Afalse%7D%2C%7B%22model%22%3A%22TaxonStatistics%22%2C%22as%22%3A%22Statistics%22%2C%22required%22%3Afalse%7D%2C%7B%22model%22%3A%22TaxonImages%22%2C%22as%22%3A%22images%22%2C%22required%22%3A${imagesRequired}%7D%5D"
    }

}


sealed class APIType {

     sealed class Request: APIType() {
       class Mushroom(val offset: Int, val limit: Int, val searchString: String?, val requirePictures: Boolean): Request()
    }

}
