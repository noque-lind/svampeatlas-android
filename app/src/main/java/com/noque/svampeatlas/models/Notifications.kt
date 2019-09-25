package com.noque.svampeatlas.models

import com.google.gson.annotations.SerializedName

data class GsonNotifications(
    val endOfRecords: Boolean,
    val results: List<Notification>
)

data class Notification(
    @SerializedName("observation_id") val observationID: Int,
    @SerializedName("validation") val observationValidation: String,
    @SerializedName("FullName") val observationFullName: String,
    @SerializedName("eventType") val eventType: String,
    @SerializedName("createdAt") val date: String,
    @SerializedName("username") val triggerName: String,
    @SerializedName("Initialer") val triggerInitials: String,
    @SerializedName("user_facebook") private val triggerFacebookID: String?,
    @SerializedName("img") private val observationImage: String?
) {

    val triggerImageURL: String? get() { return if (triggerFacebookID != null) "https://graph.facebook.com/${triggerFacebookID}/picture?width=70&height=70" else null }
    val imageURL: String? get() { return if (observationImage != null) "https://svampe.databasen.org/uploads/${observationImage}.JPG" else null }
}