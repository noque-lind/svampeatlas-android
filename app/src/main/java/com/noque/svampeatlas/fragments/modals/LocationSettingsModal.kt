package com.noque.svampeatlas.fragments.modals

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.fragment_modal_locality_settings.*

class LocationSettingsModal: DialogFragment() {

    interface Listener {
        fun lockLocalitySet(value: Boolean)
        fun lockLocationSet(value: Boolean)
    }

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
        cancelButton.apply {
            setOnClickListener {
                dismiss()
            }
        }
        localitySwitch.setOnCheckedChangeListener { _, _ ->
            (targetFragment as? Listener)?.lockLocalitySet(localitySwitch.isChecked)
        }

        locationSwitch.setOnCheckedChangeListener { _, _ ->
            (targetFragment as? Listener)?.lockLocationSet(locationSwitch.isChecked)
        }
    }
}