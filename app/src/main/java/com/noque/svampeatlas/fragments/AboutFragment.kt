package com.noque.svampeatlas.fragments

import android.app.ActionBar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.marginStart
import androidx.fragment.app.Fragment
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.dpToPx
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.views.BlankActivity
import com.noque.svampeatlas.views.HeaderView
import kotlinx.android.synthetic.main.fragment_about.*

class AboutFragment: Fragment() {


    private var linearLayout: LinearLayout? = null
    private var toolbar by autoCleared<Toolbar>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setupView()
    }

    private fun initView() {
        toolbar = aboutFragment_toolbar
        linearLayout = aboutFragment_linearLayout
    }

    private fun setupView() {
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
        createText(getString(R.string.aboutVC_recognition_title), getString(R.string.aboutVC_recognition_message))
        createText(getString(R.string.aboutVC_general_title), getString(R.string.aboutVC_general_message))
        createText(getString(R.string.aboutVC_generalTerms_title), getString(R.string.aboutVC_generalTerms_message))
        createText(getString(R.string.aboutVC_qualityAssurance_title), getString(R.string.aboutVC_qualityAssurance_message))
        createText(getString(R.string.aboutVC_guidelines_title), getString(R.string.aboutVC_guidelines_message))
    }

    private fun createText(title: String, message: String) {
        val headerView = HeaderView(context, null)
        headerView.configure(title)

        val textView = TextView(context, null, 0, R.style.AppPrimary)
        textView.text = message

        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        params.marginStart = 8.dpToPx(context)
        params.marginEnd = 8.dpToPx(context)
        params.bottomMargin = 16.dpToPx(context)
        textView.layoutParams = params

        linearLayout?.addView(headerView)
        linearLayout?.addView(textView)
    }
}