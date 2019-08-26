package com.noque.svampeatlas.View.Fragments


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.se.omapi.Session
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.noque.svampeatlas.Extensions.changeColor
import com.noque.svampeatlas.Extensions.pxToDp
import com.noque.svampeatlas.Model.State

import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.BackgroundView
import com.noque.svampeatlas.View.BlankActivity
import com.noque.svampeatlas.ViewModel.SessionViewModel
import kotlinx.android.synthetic.main.custom_toast.*
import kotlinx.android.synthetic.main.custom_toast.view.*
import kotlinx.android.synthetic.main.fragment_login.*

class LoginFragment : Fragment() {

    companion object {
        val TAG = "LoginFragment"
    }

    // Views
    lateinit var backgroundView: BackgroundView
    lateinit var initialsEditText: EditText
    lateinit var passwordEditText: EditText
    lateinit var loginButton: Button

    // View models

    lateinit var sessionViewModel: SessionViewModel

    // Listeners

    private val loginButtonClickListener = View.OnClickListener {

        if (initialsEditText.text.isNullOrEmpty()) {
            initialsEditText.setError(resources.getString(R.string.loginFragment_initialsError))
        } else if (passwordEditText.text.isNullOrEmpty()) {
            passwordEditText.setError(resources.getString(R.string.loginFragment_passwordError))
        } else {
            sessionViewModel.login(initialsEditText.text.toString(), passwordEditText.text.toString())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
        setupViewModels()

    }

    private fun initViews() {
        backgroundView = loginFragment_backgroundView
        initialsEditText = loginFragment_initialsEditText
        passwordEditText = loginFragment_passwordEditText
        loginButton = loginFragment_loginButton
    }

    private fun setupViews() {
        (requireActivity() as BlankActivity).setSupportActionBar(loginFragment_toolbar)

        loginButton.setOnClickListener(loginButtonClickListener)
    }

    private fun setupViewModels() {
        activity?.let {
            sessionViewModel = ViewModelProviders.of(it).get(SessionViewModel::class.java)

            sessionViewModel.loggedInState.observe(viewLifecycleOwner, Observer {
                backgroundView.reset()

                when (it) {
                    is State.Error -> {

                        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_elmessageview_failure)
                        createToast(it.error.title, it.error.message, bitmap.changeColor(ResourcesCompat.getColor(resources, R.color.colorRed, null)))
                    }

                    is State.Loading -> {
                        backgroundView.setLoading()
                    }
                }
            })
        }
    }

    private fun createToast(title: String, message: String, bitmap: Bitmap) {
        val container = custom_toast_container
        val layout = layoutInflater.inflate(R.layout.custom_toast, container)

        layout.customToast_titleTextView.text = title
        layout.customToast_messageTextView.text = message
        layout.customToast_imageView.setImageBitmap(bitmap)

        with (Toast(context)) {
            setGravity(Gravity.CENTER_VERTICAL, 0, 16.pxToDp(context))
            duration = Toast.LENGTH_LONG
            view = layout
            show()
        }
    }
}
