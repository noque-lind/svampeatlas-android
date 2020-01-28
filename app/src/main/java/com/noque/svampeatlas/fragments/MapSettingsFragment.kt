package com.noque.svampeatlas.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.fragment_map_settings.*

class MapSettingsFragment(private var radius: Int, private var age: Int) : DialogFragment() {

    interface Listener {
        fun newSearch()
        fun radiusChanged(value: Int)
        fun ageChanged(value: Int)
        fun clearAllSet(value: Boolean)
    }

    // Objects
    private var listener: Listener? = null

    // Views
    private lateinit var cancelButton: ImageButton
    private lateinit var switch: Switch
    private lateinit var radiusLabel: TextView
    private lateinit var radiusSlider: SeekBar
    private lateinit var ageLabel: TextView
    private lateinit var ageSlider: SeekBar
    private lateinit var searchButton: Button

    // Listeners
    private val onSeekBarChangeListener by lazy {
        object: SeekBar.OnSeekBarChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onProgressChanged(p0: SeekBar, p1: Int, p2: Boolean) {
                if (p0.id == R.id.mapSettingsFragment_radiusSlider) {
                    radius = p1 + 1000
                    listener?.radiusChanged(radius)
                    setRadiusLabel()
                } else if (p0.id == R.id.mapSettingsFragment_ageSlider) {
                    age = p1 + 1
                    listener?.ageChanged(age)
                    setAgeLabel()
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        }
    }

    private val onExitButtonPressed by lazy {
        View.OnClickListener {
            dismiss()
        }
    }

    private val onSearchButtonPressed by lazy {
        View.OnClickListener {
            listener?.newSearch()
            dismiss()
        }
    }

    private val onSwitchValueChanged by lazy {
        CompoundButton.OnCheckedChangeListener { _, value ->
            listener?.clearAllSet(value)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    private fun initViews() {
        cancelButton = mapSettingsFragment_cancelButton
        switch = mapSettingsFragment_switch
        radiusLabel = mapSettingFragment_radiusLabel
        radiusSlider = mapSettingsFragment_radiusSlider
        ageLabel = mapSettingFragment_ageLabel
        ageSlider = mapSettingsFragment_ageSlider
        searchButton = mapSettingsFragment_searchButton
    }

    private fun setupViews() {
        cancelButton.setOnClickListener(onExitButtonPressed)
        radiusSlider.setOnSeekBarChangeListener(onSeekBarChangeListener)
        ageSlider.setOnSeekBarChangeListener(onSeekBarChangeListener)
        searchButton.setOnClickListener(onSearchButtonPressed)
        switch.setOnCheckedChangeListener(onSwitchValueChanged)

        setRadiusLabel()
        setAgeLabel()

        radiusSlider.progress = radius - 1000
        ageSlider.progress = age - 1
    }

    private fun setRadiusLabel() {
        radiusLabel.text = "${(radius.toDouble() / 1000)} km."
    }

    private fun setAgeLabel() {
        ageLabel.text = "$age år."
    }
}