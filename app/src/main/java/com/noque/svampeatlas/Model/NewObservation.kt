package com.noque.svampeatlas.Model

import com.google.android.gms.maps.model.LatLng
import com.noque.svampeatlas.Utilities.ObservationQueries
import java.util.*

data class NewObservation(private val observationDate: Date = Date(),
        private val observationDateAccuracy: String = "day",
                          var substrate: Int? = null,
                          var vegetationType: Int? = null,
                          var hosts: List<Int> = listOf<Int>(),
                          var ecologyNote: String? = null,
                          var species: Mushroom? = null,
                          var confidence: String? = null,
                          var note: String? = null,
                          var coordinate: LatLng? = null,
                          val user: String? = null,
                          var locality: Locality? = null,
                          var images: List<String> = listOf<String>()) {


}