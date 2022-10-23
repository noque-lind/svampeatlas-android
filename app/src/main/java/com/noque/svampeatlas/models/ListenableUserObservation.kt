package com.noque.svampeatlas.models

import androidx.lifecycle.MutableLiveData
import com.noque.svampeatlas.utilities.SharedPreferences
import java.util.*

class Observable {
    companion object {
        fun <X>observe(
            source: MutableLiveData<X>, onCallback: ((X) -> Unit)
        ): MutableLiveData<X> {
            source.observeForever {
                onCallback(it)
            }
            return source
        }
    }
}

class ListenableUserObservation(private val onChanged: (UserObservation) -> Unit) {

    var userObservation = UserObservation()

    val images = Observable.observe(MutableLiveData<List<UserObservation.Image>>()) {
        userObservation.images = it
    }

    // Page 1 properties
    val mushroom = Observable.observe(MutableLiveData<Pair<Mushroom, DeterminationConfidence>?>()) {
        userObservation.mushroom = it
    }

    var determinationNotes: String?
        get() = userObservation.determinationNotes
        set(value) {
            userObservation.determinationNotes = value
        }


    // Page 2 properties
    val observationDate = Observable.observe(MutableLiveData<Date>()) {
        userObservation.observationDate = it
    }
    val substrate = Observable.observe(MutableLiveData<Pair<Substrate, Boolean>?>()) {
        userObservation.substrate = it
    }
    val vegetationType = Observable.observe(MutableLiveData<Pair<VegetationType, Boolean>?>()) {
        userObservation.vegetationType = it
    }

    val hosts = Observable.observe(MutableLiveData<Pair<List<Host>, Boolean>?>()) {
        userObservation.hosts = it
    }

    val notes = Observable.observe(MutableLiveData<String?>()) {
        userObservation.notes = it
    }
    val ecologyNotes = Observable.observe(MutableLiveData<String?>()) {
        userObservation.ecologyNotes = it
    }

    // Page 3 properties
    val locality = Observable.observe(MutableLiveData<Pair<Locality, Boolean>?>()) {
        userObservation.locality = it
        SharedPreferences.lockedLocality = if (it?.second == true) it.first else null
    }
    val location = Observable.observe(MutableLiveData<Pair<Location, Boolean>?>()) {
        userObservation.location = it
        SharedPreferences.lockedLocation = if (it?.second == true) it.first else null
    }


    fun set(value: UserObservation) {
        userObservation.deleteTempimages()

        userObservation = value
        onChanged.invoke(value)
        images.value = value.images
        mushroom.value = value.mushroom
        observationDate.value = value.observationDate
        substrate.value = value.substrate
        vegetationType.value = value.vegetationType
        hosts.value = value.hosts
        notes.value = value.notes
        ecologyNotes.value = value.ecologyNotes
        locality.value = value.locality
        location.value = value.location
    }
}