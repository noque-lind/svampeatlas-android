package com.noque.svampeatlas.models

import com.noque.svampeatlas.extensions.Date
import java.util.*

data class Comment(
    val id: Int,
    private val _date: String,
    val content: String,
    val commenterName: String,
    val initials: String?,
    private val _commenterFacebookID: String?) {

    val commenterProfileImageURL: String? get() {
        if (_commenterFacebookID != null) {
            return "https://graph.facebook.com/${_commenterFacebookID}/picture?width=70&height=70"
        } else {
            return null
        }
    }

    val date: Date? get() { return Date(_date) }
}