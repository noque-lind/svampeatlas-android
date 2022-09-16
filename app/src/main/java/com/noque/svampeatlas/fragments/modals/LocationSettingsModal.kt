package com.noque.svampeatlas.fragments.modals

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.fragment_modal_locality_settings.*

class LocationSettingsModal(
    private val lockedLocality: Boolean,
    private val lockedLocation: Boolean,
    private val allowLockingLocality: Boolean
) : DialogFragment() {

    interface Listener {
        fun lockLocalitySet(value: Boolean)
        fun lockLocationSet(value: Boolean)
    }

    private lateinit var saveButton: Button
    private lateinit var cancelButton: ImageButton
    private lateinit var locationSwitch: SwitchMaterial
    private lateinit var localitySwitch: SwitchMaterial

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        return inflater.inflate(R.layout.fragment_modal_locality_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        saveButton = localitySettingsFragment_saveButton
        cancelButton = localitySettingsFragment_cancelButton
        locationSwitch = localitySettingsFragment_locationSwitch
        localitySwitch = localitySettingsFragment_localitySwitch

        setupViews()
    }


    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun setupViews() {
        locationSwitch.isChecked = lockedLocation
        localitySwitch.isChecked = lockedLocality

        if (!allowLockingLocality) localitySwitch.visibility = View.GONE

        locationSwitch.setOnCheckedChangeListener { _, newValue ->
            if (newValue && localitySwitch.visibility == View.VISIBLE) localitySwitch.isChecked = true
        }

        saveButton.setOnClickListener {
            (targetFragment as? Listener)?.lockLocalitySet(localitySwitch.isChecked)
            (targetFragment as? Listener)?.lockLocationSet(locationSwitch.isChecked)
            dismiss()
        }

        cancelButton.apply {
            setOnClickListener {
                dismiss()
            }
        }
    }
}