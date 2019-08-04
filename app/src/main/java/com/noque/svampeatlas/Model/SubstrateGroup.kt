package com.noque.svampeatlas.Model

import com.noque.svampeatlas.Utilities.APIType

data class SubstrateGroup(val dkName: String,
                          val enName: String,
                          private var _substrates: MutableList<Substrate>) {

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

    val substrates: List<Substrate> get() = _substrates

    fun appendSubstrate(substrate: Substrate) {
        _substrates.add(substrate)
    }
}