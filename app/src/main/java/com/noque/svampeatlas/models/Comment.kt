package com.noque.svampeatlas.models

data class Comment(
    val id: Int,
    val date: String,
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
}