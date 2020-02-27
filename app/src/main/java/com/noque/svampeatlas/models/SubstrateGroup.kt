package com.noque.svampeatlas.models

import com.noque.svampeatlas.extensions.isDanish
import java.util.*

data class SubstrateGroup(val dkName: String,
                          val enName: String,
                          val substrates: MutableList<Substrate>) {

    val id: Int get() {
        when (dkName) {
            "jord" -> return 0
            "ved" -> return 1
            "plantemateriale" -> return 2
            "mosser" -> return 3
            "dyr" -> return 4
            "svampe og svampedyr" -> return 5
            "sten" -> return 6
            else -> return 100
        }
    }

    val localizedName: String get() {
        return if (Locale.getDefault().isDanish()) {
            dkName
        } else {
            enName
        }
    }

    fun appendSubstrate(substrate: Substrate) {
        substrates.add(substrate)
    }
}