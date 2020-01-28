package com.noque.svampeatlas.fragments.add_observation

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.adapters.add_observation.SpeciesAdapter
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.R
import com.noque.svampeatlas.fragments.AddObservationFragmentDirections
import com.noque.svampeatlas.fragments.DetailsFragment
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.PredictionResult
import com.noque.svampeatlas.views.SearchBarListener
import com.noque.svampeatlas.views.SearchBarView
import com.noque.svampeatlas.view_models.MushroomsViewModel
import com.noque.svampeatlas.view_models.factories.MushroomsViewModelFactory
import com.noque.svampeatlas.view_models.NewObservationViewModel
import kotlinx.android.synthetic.main.fragment_add_observation_specie.*

class SpeciesFragment : Fragment() {

    companion object {
        val TAG = "AddObs.SpeciesFragment"
    }

    // Objects

    private var defaultState: Boolean = true

    // Views
    private var recyclerView: RecyclerView? = null
    private var searchBar: SearchBarView? = null

    // View models
    private val mushroomViewModel by lazy {
        ViewModelProviders.of(
            this,
            MushroomsViewModelFactory(
                MushroomsViewModel.Category.FAVORITES,
                requireActivity().application
            )
        ).get(MushroomsViewModel::class.java)
    }
    private val newObservationViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(NewObservationViewModel::class.java)
    }

    // Adapters

    private val speciesAdapter: SpeciesAdapter by lazy {
        val adapter = SpeciesAdapter()

        adapter.setListener(object : SpeciesAdapter.Listener {
            override fun mushroomSelected(mushroom: Mushroom) {
                if (mushroom.id == newObservationViewModel.mushroom.value?.first?.id) {
                    val action =
                        AddObservationFragmentDirections.actionGlobalMushroomDetailsFragment(
                            mushroom.id,
                            DetailsFragment.TakesSelection.DESELECT,
                            DetailsFragment.Type.SPECIES,
                            null,
                            null
                        )
                    findNavController().navigate(action)
                } else {
                    val action =
                        AddObservationFragmentDirections.actionGlobalMushroomDetailsFragment(
                            mushroom.id,
                            DetailsFragment.TakesSelection.SELECT,
                            DetailsFragment.Type.SPECIES,
                            null,
                            null
                        )
                    findNavController().navigate(action)
                }
            }

            override fun confidenceSet(confidence: NewObservationViewModel.DeterminationConfidence) {
                newObservationViewModel.setConfidence(confidence)
            }
        })

        adapter
    }

    // Listeners

    private val searchBarListener = object : SearchBarListener {
        override fun newSearch(entry: String) {
            defaultState = false
            mushroomViewModel.search(entry, false, true)
        }

        override fun clearedSearchEntry() {
            defaultState()
        }
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if (!recyclerView.canScrollVertically(-1)) {
                searchBar?.expand()
            } else if (dy > 0) {
                searchBar?.collapse()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_observation_specie, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupView()
        setupViewModels()
    }

    private fun initViews() {
        recyclerView = addObservationSpecieFragment_recyclerView
        searchBar = addObservationSpecieFragment_searchBarView
    }

    private fun setupView() {
        searchBar?.apply {
            setListener(searchBarListener)
        }

        recyclerView?.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = speciesAdapter
            addOnScrollListener(onScrollListener)
        }
    }

    private fun setupViewModels() {
        newObservationViewModel.mushroom.observe(viewLifecycleOwner, Observer {

            if (it != null) {
                recyclerView?.setPadding(0, 0, 0, 0)
                searchBar?.visibility = View.GONE
                speciesAdapter.configureUpperSection(
                    State.Items(
                        listOf(
                            SpeciesAdapter.Item.SelectedMushroom(
                                it.first,
                                it.second
                            )
                        )
                    ),
                    if (it.first.isGenus) getString(R.string.speciesFragment_section_choosen_genus) else getString(
                        R.string.speciesFragment_section_choosen_species)
                )
            } else {
                recyclerView?.setPadding(0, (resources.getDimension(R.dimen.searchbar_view_height) + resources.getDimension(
                        R.dimen.searchbar_top_margin
                    )).toInt(), 0, 0)
                searchBar?.visibility = View.VISIBLE
                searchBar?.expand()
                speciesAdapter.configureUpperSection(
                    State.Items(
                        listOf(
                            SpeciesAdapter.Item.UnknownSpecies()
                        )
                    ),
                    null
                )
            }

            recyclerView?.scrollToPosition(0)
        })


        newObservationViewModel.predictionResultsState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Items -> {
                    speciesAdapter.configureSuggestionsSection(State.Items(it.items.map { SpeciesAdapter.Item.SelectableMushroom(it.mushroom, it.score) }), getString(R.string.speciesFragment_section_suggestions))
                }

                is State.Error -> {
                    speciesAdapter.configureSuggestionsSection(State.Error(it.error), getString(R.string.speciesFragment_section_suggestions))
                }

                is State.Loading -> {
                    speciesAdapter.configureSuggestionsSection(State.Loading(), getString(R.string.speciesFragment_section_suggestions))
                }

                is State.Empty -> {
                    speciesAdapter.configureSuggestionsSection(State.Empty(), null)
                }
            }
            })


        mushroomViewModel.mushroomsState.observe(viewLifecycleOwner, Observer {

            // We only want the mushroomState to to anything to the UI if no mushroom has been selected
                when (it) {
                    is State.Items -> {
                      val items = it.items.map { SpeciesAdapter.Item.SelectableMushroom(it) }

                        if (defaultState) {
                            speciesAdapter.configureMiddleSectionState(State.Items(items), getString(R.string.speciesFragment_section_favorites))
                        } else {
                            speciesAdapter.configureMiddleSectionState(State.Items(items), getString(R.string.speciesFragment_section_search_results))
                        }
                    }

                    is State.Loading -> {
                        if (defaultState) {
                            speciesAdapter.configureMiddleSectionState(State.Loading(), getString(R.string.speciesFragment_section_favorites))
                        } else {
                            speciesAdapter.configureMiddleSectionState(State.Loading(), getString(R.string.speciesFragment_section_search_results))
                        }
                    }

                    is State.Error -> {
                        if (defaultState) {
                            speciesAdapter.configureMiddleSectionState(State.Empty(), null)
                        } else {
                            speciesAdapter.configureMiddleSectionState(State.Error(it.error), getString(R.string.speciesFragment_section_search_results))
                        }
                    }

                    is State.Empty -> {
                        defaultState()
                    }
                }
        })
    }

    private fun defaultState() {
        defaultState = true
        mushroomViewModel.selectCategory(MushroomsViewModel.Category.FAVORITES, true)
    }

    override fun onPause() {
        Log.d(TAG, "On Pause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "On Stop")
        super.onStop()
    }

    override fun onDestroyView() {
        Log.d(TAG, "OnDestroy View")

        recyclerView?.adapter = null
        searchBar?.setListener(null)

        recyclerView = null
        searchBar = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        Log.d(TAG, "On Destroy")
        super.onDestroy()
    }

    override fun onDetach() {
        Log.d(TAG, "On Detach")
        super.onDetach()
    }

}