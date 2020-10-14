package com.noque.svampeatlas.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.noque.svampeatlas.R
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.views.BlankActivity
import kotlinx.android.synthetic.main.fragment_settings.*

class SettingsFragment: Fragment() {

    private var toolbar by autoCleared<androidx.appcompat.widget.Toolbar>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupView()
    }

    private fun initViews() {
        toolbar = settingsFragment_toolbar
    }

    private fun setupView() {
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
    }
}