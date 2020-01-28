package com.noque.svampeatlas.fragments


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
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
import androidx.lifecycle.ViewModelProviders
import com.noque.svampeatlas.extensions.changeColor
import com.noque.svampeatlas.extensions.pxToDp
import com.noque.svampeatlas.models.State

import com.noque.svampeatlas.R
import com.noque.svampeatlas.views.BackgroundView
import com.noque.svampeatlas.views.BlankActivity
import com.noque.svampeatlas.view_models.SessionViewModel
import kotlinx.android.synthetic.main.custom_toast.*
import kotlinx.android.synthetic.main.custom_toast.view.*
import kotlinx.android.synthetic.main.fragment_login.*
import androidx.core.content.ContextCompat.getSystemService
import android.content.Context
import android.view.inputmethod.InputMethodManager
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.noque.svampeatlas.utilities.GlideApp
import java.lang.Exception


class LoginFragment : Fragment() {

    companion object {
        val TAG = "LoginFragment"
    }

    // Views
    private lateinit var backgroundView: BackgroundView
    private lateinit var initialsEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var createAccountButton: Button
    private lateinit var bg: ImageView

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

        getSystemService(requireContext(), InputMethodManager::class.java)?.let {
            it.hideSoftInputFromWindow(
                requireActivity().currentFocus?.windowToken, InputMethodManager.HIDE_NOT_ALWAYS
            )
        }

        view?.requestFocus()
    }

    private val createAccountButtonPressed = View.OnClickListener {
        try {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse("https://svampe.databasen.org/signup"))
            startActivity(intent)
        } catch (exception: ActivityNotFoundException) {}
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
        createAccountButton = loginFragment_createAccountButton
        bg = loginFragment_bg
    }

    private fun setupViews() {
        (requireActivity() as BlankActivity).setSupportActionBar(loginFragment_toolbar)

        loginButton.setOnClickListener(loginButtonClickListener)
        createAccountButton.setOnClickListener(createAccountButtonPressed)

        GlideApp.with(requireContext()).load(R.drawable.background).transition(DrawableTransitionOptions.withCrossFade()).into(bg)
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

    fun hideSoftKeyboard(activity: Activity, view: View) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.applicationWindowToken, 0)
    }
}
