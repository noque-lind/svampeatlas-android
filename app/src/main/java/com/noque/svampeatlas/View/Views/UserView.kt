package com.noque.svampeatlas.View.Views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.noque.svampeatlas.Model.User
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_user.view.*

class UserView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private val primaryTextView: TextView
    private val secondaryTextView: TextView
    private val profileImageView: ProfileImageView


    init {
    val inflater = LayoutInflater.from(getContext())
    val view = inflater.inflate(R.layout.view_user, this)
        primaryTextView = view.userView_primaryTextView
        profileImageView = view.userView_profileImageView
        secondaryTextView = view.userView_secondaryTextView
}


    fun configure(user: User) {
        profileImageView.configure(user.initials, user.imageURL)
        secondaryTextView.visibility = View.GONE
        primaryTextView.text = user.name
    }

    fun configureAsGuest() {
        primaryTextView.text = resources.getText(R.string.application_title)
        secondaryTextView.text = resources.getText(R.string.menu_guest)
        secondaryTextView.visibility = View.VISIBLE
        profileImageView.configure(null, null)
    }
}