package com.noque.svampeatlas.fragments.add_observation

import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.adapters.add_observation.details_picker.HostsAdapter
import com.noque.svampeatlas.adapters.add_observation.details_picker.SubstratesAdapter
import com.noque.svampeatlas.adapters.add_observation.details_picker.VegetationTypesAdapter

import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.R
import com.noque.svampeatlas.views.BackgroundView
import com.noque.svampeatlas.view_models.NewObservationViewModel
import com.noque.svampeatlas.view_models.DetailsPickerViewModel
import kotlinx.android.synthetic.main.fragment_details_picker.*
import android.widget.ImageButton
import com.noque.svampeatlas.adapters.PickerAdapter
import com.noque.svampeatlas.extensions.capitalized
import com.noque.svampeatlas.extensions.upperCased
import com.noque.svampeatlas.view_models.factories.DetailsPickerViewModelFactory


class DetailsPickerFragment() : DialogFragment() {

    companion object {
        const val TYPEKEY = "DETAILSPICKERFRAGMENT_TYPEKEY"
    }

    enum class Type {
        SUBSTRATEPICKER,
        VEGETATIONTYPEPICKER,
        HOSTPICKER
    }

    // Objects

    private lateinit var type: Type


    // Views

    private lateinit var backgroundView: BackgroundView
    private lateinit var recyclerView: RecyclerView
    private lateinit var titleTextView: TextView
    private lateinit var switch: Switch
    private lateinit var cancelButton: ImageButton


    // Adapters


    private val substratesAdapter: SubstratesAdapter by lazy {
        val adapter = SubstratesAdapter()
        adapter.setListener(object: PickerAdapter.Listener<Substrate> {
            override fun itemSelected(item: Substrate) {
                newObservationViewModel.setSubstrate(item, switch.isChecked)
                dismiss()
            }

            override fun itemDeselected(item: Substrate) {}
        })
        adapter
    }

    private val vegetationTypesAdapter: VegetationTypesAdapter by lazy {
        val adapter = VegetationTypesAdapter()

        adapter.setListener(object: PickerAdapter.Listener<VegetationType> {
            override fun itemDeselected(item: VegetationType) {}
            override fun itemSelected(item: VegetationType) {
                newObservationViewModel.setVegetationType(item, switch.isChecked)
                dismiss()
            }

        })
        adapter
    }

    private val hostsAdapter: HostsAdapter by lazy {
        val adapter = HostsAdapter()
        adapter.setListener(object: PickerAdapter.Listener<Host> {
            override fun itemSelected(item: Host) {
                newObservationViewModel.appendHost(item, switch.isChecked)
            }

            override fun itemDeselected(item: Host) {
                newObservationViewModel.removeHost(item, switch.isChecked)
            }
        })

        adapter
    }


    // View models

    private val newObservationViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(NewObservationViewModel::class.java)
    }

    private val observationDetailsPickerViewModel by lazy {
            ViewModelProviders.of(this, DetailsPickerViewModelFactory(type, requireActivity().application)).get(DetailsPickerViewModel::class.java)
    }


    private val onExitButtonPressed by lazy {
        View.OnClickListener {
            when (type) {
                Type.HOSTPICKER -> {
                    newObservationViewModel.setHostsLockedState(switch.isChecked)
                }
                else -> {}
            }

            dismiss()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        return inflater.inflate(R.layout.fragment_details_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        type = arguments?.getSerializable(TYPEKEY) as Type
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
        setupViewModels()
    }


    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.70).toInt()
        dialog?.window?.setLayout(width, height)
    }

    private fun initViews() {
        backgroundView = detailsPickerFragment_backgroundView
        recyclerView = detailsPickerFragment_recyclerView
        titleTextView = detailsPickerFragment_headerTextView
        switch = detailsPickerFragment_switch
        cancelButton = detailsPickerFragment_cancelButton
    }

    private fun setupViews() {
        cancelButton.apply {
            setOnClickListener(onExitButtonPressed)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
        }


        when (type) {
            Type.VEGETATIONTYPEPICKER -> {
                recyclerView.adapter = vegetationTypesAdapter
                titleTextView.text = resources.getString(R.string.detailsPickerFragment_vegetationTypesPicker)
            }

            Type.SUBSTRATEPICKER -> {
                recyclerView.adapter = substratesAdapter
                titleTextView.text = resources.getString(R.string.detailsPickerFragment_substratePicker)
            }

            Type.HOSTPICKER -> {
                cancelButton.setImageResource(R.drawable.glyph_checkmark)
                recyclerView.adapter = hostsAdapter
                titleTextView.text = resources.getString(R.string.detailsPickerFragment_hostsPicker)
            }
        }
    }


    private fun setupViewModels() {
        when (type) {
            Type.VEGETATIONTYPEPICKER -> {
                switch.isChecked = newObservationViewModel.vegetationType.value?.second ?: false
            }
            Type.SUBSTRATEPICKER -> {
                switch.isChecked = newObservationViewModel.substrate.value?.second ?: false
            }
            Type.HOSTPICKER -> {
                switch.isChecked = newObservationViewModel.hosts.value?.second ?: false
            }
        }



        observationDetailsPickerViewModel.hostsState.observe(viewLifecycleOwner, Observer { state ->
            backgroundView.reset()
            when (state) {
                is State.Loading -> { backgroundView.setLoading() }
                is State.Error -> backgroundView.setError(state.error)
                is State.Items -> {
                    val selectedPositions = mutableListOf<Int>()

                    newObservationViewModel.hosts.value?.first?.forEach {
                        val index = state.items.indexOf(it)
                        if (index != -1) selectedPositions.add(index)
                    }

                    hostsAdapter.configure(listOf(Section.Builder<PickerAdapter.Item<Host>>().items(state.items.map { PickerAdapter.Item(it) }).build()), selectedPositions)
                }
            }
        })

        observationDetailsPickerViewModel.substrateGroupsState.observe(
            viewLifecycleOwner,
            Observer { state ->
                backgroundView.reset()
                when (state) {
                    is State.Loading -> { backgroundView.setLoading() }
                    is State.Error -> backgroundView.setError(state.error)
                    is State.Items -> {
                        val sections = state.items.map {
                            Section.Builder<PickerAdapter.Item<Substrate>>().title(it.localizedName.capitalized()).items(it.substrates.map { PickerAdapter.Item(it) }).build()
                        }

                        substratesAdapter.configure(sections)
                    }
                }
            })

        observationDetailsPickerViewModel.vegetationTypesState.observe(
            viewLifecycleOwner,
            Observer { state ->
                backgroundView.reset()
                when (state) {
                    is State.Loading -> { backgroundView.setLoading() }
                    is State.Error -> backgroundView.setError(state.error)
                    is State.Items -> {
                        vegetationTypesAdapter.configure(
                            listOf(Section.Builder<PickerAdapter.Item<VegetationType>>().items(state.items.map { PickerAdapter.Item(it) }).build())
                        )
                    }
                }
            })
    }

}