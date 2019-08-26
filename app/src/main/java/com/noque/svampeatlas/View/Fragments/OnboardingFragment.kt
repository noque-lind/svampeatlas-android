package com.noque.svampeatlas.View.Fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.Views.SpinnerView
import kotlinx.android.synthetic.main.fragment_onboarding.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
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
