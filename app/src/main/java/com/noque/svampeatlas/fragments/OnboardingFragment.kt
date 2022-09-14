package com.noque.svampeatlas.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.noque.svampeatlas.R
import com.noque.svampeatlas.views.SpinnerView
import kotlinx.android.synthetic.main.fragment_onboarding.*

class OnboardingFragment : Fragment() {

    private lateinit var spinnerView: SpinnerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
    }

    private fun initViews() {
        spinnerView = onboardingFragment_spinnerView
    }

    private fun setupViews() {
        spinnerView.startLoading()
    }
}
