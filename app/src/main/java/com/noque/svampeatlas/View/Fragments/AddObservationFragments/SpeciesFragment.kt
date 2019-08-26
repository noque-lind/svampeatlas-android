package com.noque.svampeatlas.View.Fragments.AddObservationFragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Adapters.AddObservationAdapters.SpeciesAdapter
import com.noque.svampeatlas.Extensions.pxToDp
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Model.Section
import com.noque.svampeatlas.Model.State
import com.noque.svampeatlas.MushroomNavigationDirections
import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.Fragments.AddObservationFragmentDirections
import com.noque.svampeatlas.View.Fragments.DetailsFragment
import com.noque.svampeatlas.View.Views.SearchBarListener
import com.noque.svampeatlas.View.Views.SearchBarView
import com.noque.svampeatlas.ViewModel.DetailsViewModel
import com.noque.svampeatlas.ViewModel.MushroomsViewModel
import com.noque.svampeatlas.ViewModel.NewObservationViewModel
import kotlinx.android.synthetic.main.fragment_add_observation_specie.*

class SpeciesFragment: Fragment() {

    companion object {
        val TAG = "AddObs.SpeciesFragment"
    }

    // Views
    private var recyclerView: RecyclerView? = null
    private var searchBar: SearchBarView? = null

    // View models
    lateinit var mushroomViewModel: MushroomsViewModel
    lateinit var newObservationViewModel: NewObservationViewModel

    // Adapters

    private val speciesAdapter: SpeciesAdapter by lazy {
        val adapter = SpeciesAdapter()

        adapter.mushroomSelected = {
            val action: MushroomNavigationDirections.ActionGlobalMushroomDetailsFragment

            if (it.id == newObservationViewModel.mushroom.value?.first?.id) {
                action = AddObservationFragmentDirections.actionGlobalMushroomDetailsFragment(it.id, DetailsFragment.TakesSelection.DESELECT, DetailsFragment.Type.SPECIES)
            } else {
                action = AddObservationFragmentDirections.actionGlobalMushroomDetailsFragment(it.id, DetailsFragment.TakesSelection.SELECT, DetailsFragment.Type.SPECIES)
            }

            findNavController().navigate(action)
        }

        adapter.confidenceSet = {
            Log.d("SpeciesFragment", it.toString())
            newObservationViewModel.setConfidence(it)
        }

        adapter
    }

    // Listeners

    private val searchBarListener = object: SearchBarListener {
        override fun newSearch(entry: String) {
            mushroomViewModel.search(entry)
        }

        override fun clearedSearchEntry() {

        }
    }

    private val onScrollListener = object: RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if(!recyclerView.canScrollVertically(-1)) {
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
        initViews()
        setupView()
        setupViewModels()
        super.onViewCreated(view, savedInstanceState)
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
        activity?.let {
            newObservationViewModel = ViewModelProviders.of(it).get(NewObservationViewModel::class.java)
            newObservationViewModel.mushroom.observe(viewLifecycleOwner, Observer {
                Log.d("SpeciesFragment", "Reading new mushroom choosen: ${it?.first?.fullName}, ${it?.second}")

                if (it != null) {
                    recyclerView?.setPadding(0,0,0,0)
                    searchBar?.visibility = View.GONE
                    searchBar?.resetText()
                    speciesAdapter.configure(listOf(Section("Valgt art", listOf(SpeciesAdapter.Item(SpeciesAdapter.Item.ViewType.SELECTEDSPECIE, it.first, it.second)))))
                } else {
                    Log.d("SpeciesFragment", "SearchBar height: ${searchBar?.height}")
                    recyclerView?.setPadding(0, (resources.getDimension(R.dimen.searchbar_view_height) + resources.getDimension(R.dimen.searchbar_top_margin)).toInt(), 0, 0)
                    searchBar?.visibility = View.VISIBLE
                    speciesAdapter.configure(listOf())
                }
            })

            mushroomViewModel = ViewModelProviders.of(it).get(MushroomsViewModel::class.java)
            mushroomViewModel.mushroomsState.observe(viewLifecycleOwner, Observer {

                // We only want the mushroomState to to anything to the UI if no mushroom has been selected
                if (newObservationViewModel.mushroom.value == null) {
                    when (it) {
                        is State.Items ->  {
                            val items = it.items.map { SpeciesAdapter.Item(SpeciesAdapter.Item.ViewType.SELECTABLE, it) }
                            speciesAdapter.configure(listOf(Section(null, items)))
                        }
                    }
                }
            })

            // Note using this instead of viewModelProvider is intentional due to the fact that i only want this to happen when the value actually
            // gets changed, and not when the fragments view gets created.
            mushroomViewModel.selectedMushroom.observe(viewLifecycleOwner, Observer {
                Log.d(TAG, "Observed selectedMushroom ${it?.fullName}")
                newObservationViewModel.setMushroom(it)
            })
        }
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
        super.onDestroyView()
        recyclerView = null
        searchBar = null
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