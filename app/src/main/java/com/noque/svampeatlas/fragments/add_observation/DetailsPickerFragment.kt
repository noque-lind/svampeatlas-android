package com.noque.svampeatlas.fragments.add_observation

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.noque.svampeatlas.adapters.add_observation.details_picker.PickerAdapter
import com.noque.svampeatlas.extensions.capitalized
import com.noque.svampeatlas.view_models.factories.DetailsPickerViewModelFactory
import com.noque.svampeatlas.views.SearchBarListener
import com.noque.svampeatlas.views.SearchBarView


class DetailsPickerFragment() : DialogFragment() {

    companion object {
        const val TYPE_KEY = "DETAILSPICKERFRAGMENT_TYPEKEY"
    }

    enum class Type {
        SUBSTRATEPICKER,
        VEGETATIONTYPEPICKER,
        HOSTPICKER
    }

    // Objects
    private lateinit var type: Type

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var titleTextView: TextView
    private lateinit var switch: SwitchMaterial
    private lateinit var cancelButton: ImageButton
    private lateinit var searchBarView: SearchBarView


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
    private val newObservationViewModel by viewModels<NewObservationViewModel>({requireParentFragment().requireParentFragment()})
    private val observationDetailsPickerViewModel by lazy {
        ViewModelProvider(this, DetailsPickerViewModelFactory(type, requireActivity().application)).get(DetailsPickerViewModel::class.java)
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
        type = arguments?.getSerializable(TYPE_KEY) as Type
        super.onViewCreated(view, savedInstanceState)
        recyclerView = detailsPickerFragment_recyclerView
        titleTextView = detailsPickerFragment_headerTextView
        switch = detailsPickerFragment_switch
        cancelButton = detailsPickerFragment_cancelButton
        searchBarView = detailsPickerFragment_searchBarView
        setupViews()
        setupViewModels()
    }


    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.70).toInt()
        dialog?.window?.setLayout(width, height)
    }

    private fun setupViews() {
        cancelButton.apply {
            setOnClickListener {
                when (type) {
                    Type.HOSTPICKER -> {
                        newObservationViewModel.setHostsLockedState(switch.isChecked)
                    }
                    else -> {}
                }
                dismiss()
            }
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            if (type == Type.HOSTPICKER) {
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        if (!recyclerView.canScrollVertically(-1)) {
                            searchBarView.expand()
                        } else if (dy > 0) {
                            searchBarView.collapse()
                        }
                    }
                })

            }

            when (type) {
                Type.VEGETATIONTYPEPICKER -> {
                    recyclerView.adapter = vegetationTypesAdapter
                    titleTextView.text =
                        resources.getString(R.string.detailsPickerFragment_vegetationTypesPicker)
                    searchBarView.visibility = View.GONE
                    recyclerView.setPadding(0, 0, 0, 0)
                }

                Type.SUBSTRATEPICKER -> {
                    recyclerView.adapter = substratesAdapter
                    titleTextView.text =
                        resources.getString(R.string.detailsPickerFragment_substratePicker)
                    searchBarView.visibility = View.GONE
                    recyclerView.setPadding(0, 0, 0, 0)
                }

                Type.HOSTPICKER -> {
                    cancelButton.setImageResource(R.drawable.glyph_checkmark)
                    recyclerView.adapter = hostsAdapter
                    titleTextView.text =
                        resources.getString(R.string.detailsPickerFragment_hostsPicker)
                    recyclerView.setPadding(
                        0,
                        (resources.getDimension(R.dimen.searchbar_view_height) + resources.getDimension(
                            R.dimen.searchbar_top_margin
                        ) * 2).toInt(),
                        0,
                        0
                    )
                    searchBarView.apply {
                        visibility = View.VISIBLE
                        setPlaceholder(resources.getString(R.string.searchVC_searchBar_placeholder))
                        setListener(object : SearchBarListener {
                            override fun newSearch(entry: String) {
                                observationDetailsPickerViewModel.getHosts(entry)
                            }

                            override fun clearedSearchEntry() {
                                observationDetailsPickerViewModel.getHosts(null)
                            }
                        })
                    }
                }
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
            when (state) {
                is State.Loading -> {
                    hostsAdapter.configure(listOf(Section(null, State.Loading())))
                }
                is State.Error -> {
                    hostsAdapter.configure(listOf(Section(null, State.Error(state.error))))
                }
                is State.Items -> {
                    if (state.items.second) {
                        val defaultList = state.items.first.filterNot { it.isUserSelected }
                        val previouslyUsed = state.items.first.filter { it.isUserSelected }
                        hostsAdapter.configure(listOf(
                            Section(if (previouslyUsed.isNotEmpty()) getString(R.string.common_previouslyUsed) else null, State.Items(previouslyUsed.map { PickerAdapter.PickerItem(it) })),
                            Section(getString(R.string.common_mostUsed), State.Items(defaultList.map { PickerAdapter.PickerItem(it) }))
                        ), newObservationViewModel.hosts.value?.first ?: listOf())
                    } else {
                        hostsAdapter.configure(listOf(Section(null, State.Items(state.items.first.map { PickerAdapter.PickerItem(it) }))), newObservationViewModel.hosts.value?.first ?: mutableListOf())
                    }
                }
            }
        })

        observationDetailsPickerViewModel.substrateGroupsState.observe(
            viewLifecycleOwner,
            Observer { state ->
                when (state) {
                    is State.Loading -> { substratesAdapter.configure(listOf(Section(null, State.Loading()))) }
                    is State.Error -> { substratesAdapter.configure(listOf(Section(null, State.Error(state.error)))) }
                    is State.Items -> {
                        val sections = state.items.map {
                            Section.Builder<PickerAdapter.PickerItem<Substrate>>().title(it.localizedName.capitalized()).items(it.substrates.map { PickerAdapter.PickerItem(it) }).build()
                        }

                        substratesAdapter.configure(sections)
                    }
                }
            })

        observationDetailsPickerViewModel.vegetationTypesState.observe(
            viewLifecycleOwner,
            Observer { state ->
                when (state) {
                    is State.Loading -> vegetationTypesAdapter.configure(listOf(Section(null, State.Loading())))
                    is State.Error -> vegetationTypesAdapter.configure(listOf(Section(null, State.Error(state.error))))
                    is State.Items -> {
                        vegetationTypesAdapter.configure(
                            listOf(Section.Builder<PickerAdapter.PickerItem<VegetationType>>().items(state.items.map { PickerAdapter.PickerItem(it) }).build())
                        )
                    }
                }
            })
    }
}