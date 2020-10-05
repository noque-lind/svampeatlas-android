package com.noque.svampeatlas.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.noque.svampeatlas.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
    }
}