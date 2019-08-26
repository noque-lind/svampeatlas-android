package com.noque.svampeatlas.View.Fragments.AddObservationFragments

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
import com.noque.svampeatlas.Adapters.AddObservationAdapters.DetailsPickerAdapters.HostsAdapter
import com.noque.svampeatlas.Adapters.AddObservationAdapters.DetailsPickerAdapters.SubstratesAdapter
import com.noque.svampeatlas.Adapters.AddObservationAdapters.DetailsPickerAdapters.VegetationTypesAdapter

import com.noque.svampeatlas.Model.*
import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.BackgroundView
import com.noque.svampeatlas.ViewModel.NewObservationViewModel
import com.noque.svampeatlas.ViewModel.ObservationDetailsPickerViewModel
import kotlinx.android.synthetic.main.fragment_details_picker.*


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


    // Adapters


    private val substratesAdapter: SubstratesAdapter by lazy {
        val adapter = SubstratesAdapter()
        adapter.itemSelected = {

            newObservationViewModel.setSubstrate(it, switch.isChecked)
            dismiss()
        }
        adapter
    }

    private val vegetationTypesAdapter: VegetationTypesAdapter by lazy {
        val adapter = VegetationTypesAdapter()

        adapter.itemSelected = {
            newObservationViewModel.setVegetationType(it, switch.isChecked)
            dismiss()
        }
        adapter
    }

    private val hostsAdapter: HostsAdapter by lazy {
        val adapter = HostsAdapter()
        adapter.itemSelected = {
            newObservationViewModel.appendHost(it, switch.isChecked)
        }

        adapter.itemDeSelected = {
            newObservationViewModel.removeHost(it, switch.isChecked)
        }
        adapter
    }


    // View models

    private lateinit var newObservationViewModel: NewObservationViewModel
    private lateinit var observationDetailsPickerViewModel: ObservationDetailsPickerViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        type = arguments?.getSerializable(TYPEKEY) as Type
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
        setupViewModels()

        when (type) {
            Type.SUBSTRATEPICKER -> observationDetailsPickerViewModel.getSubstrateGroups()
            Type.VEGETATIONTYPEPICKER -> observationDetailsPickerViewModel.getVegetationTypes()
            Type.HOSTPICKER -> observationDetailsPickerViewModel.getHosts()
        }

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
    }

    private fun setupViews() {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            dividerItemDecoration.setDrawable(ColorDrawable(resources.getColor(R.color.colorPrimary)))
            addItemDecoration(dividerItemDecoration)
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
                recyclerView.adapter = hostsAdapter
                titleTextView.text = resources.getString(R.string.detailsPickerFragment_hostsPicker)
            }
        }
    }


    private fun setupViewModels() {
        newObservationViewModel =
            ViewModelProviders.of(requireActivity()).get(NewObservationViewModel::class.java)


        observationDetailsPickerViewModel =
            ViewModelProviders.of(this).get(ObservationDetailsPickerViewModel::class.java)

        observationDetailsPickerViewModel.hostsState.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is State.Error -> backgroundView.setError(state.error)
                is State.Items -> {
                    val selectedPositions = mutableListOf<Int>()

                    newObservationViewModel.hosts.value?.first?.forEach {
                        val index = state.items.indexOf(it)
                        if (index != -1) selectedPositions.add(index)
                    }

                    hostsAdapter.configure(listOf(Section(null, state.items)), selectedPositions)
                }
            }
        })

        observationDetailsPickerViewModel.substrateGroupsState.observe(
            viewLifecycleOwner,
            Observer { state ->
                when (state) {
                    is State.Error -> backgroundView.setError(state.error)
                    is State.Items -> {
                        val sections = state.items.map {
                            Section(it.dkName, it.substrates)
                        }
                        substratesAdapter.configure(sections)
                    }
                }
            })

        observationDetailsPickerViewModel.vegetationTypesState.observe(
            viewLifecycleOwner,
            Observer { state ->
                when (state) {
                    is State.Error -> backgroundView.setError(state.error)
                    is State.Items -> vegetationTypesAdapter.configure(
                        listOf(
                            Section(
                                null,
                                state.items
                            )
                        )
                    )
                }
            })
    }

}