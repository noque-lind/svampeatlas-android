package com.noque.svampeatlas.View.Fragments.AddObservationFragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Adapters.AddObservationAdapters.AddObservationSpecieAdapter
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.Model.State
import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.Views.SearchBarDelegate
import com.noque.svampeatlas.View.Views.SearchBarView
import com.noque.svampeatlas.ViewModel.MushroomsViewModel
import kotlinx.android.synthetic.main.fragment_add_observation_specie.*

class SpeciesFragment: Fragment() {

    lateinit var mushroomViewModel: MushroomsViewModel
    lateinit var recyclerView: RecyclerView


    lateinit var searchBar: SearchBarView

    private val searchBarListener = object: SearchBarDelegate {
        override fun newSearch(entry: String) {
            Log.d("Something happended", "SearchBar")
            mushroomViewModel.search(entry)
        }

        override fun clearedSearchEntry() {

        }

    }

    private val observer = object:  Observer<State<List<Mushroom>>> {
        override fun onChanged(t: State<List<Mushroom>>?) {
            when (t) {
                is State.Items ->  {
                    Log.d("AddObservation", "something happended")
                    adapter.configure(listOf(AddObservationSpecieAdapter.Section.SelectableMushroom("Lol", t.items)))
                }
            }
        }

    }

    private val adapter: AddObservationSpecieAdapter by lazy { AddObservationSpecieAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_observation_specie, container, false)
    }

    private fun bindViews() {
        searchBar = addObservationSpecieFragment_searchBarView
        recyclerView = addObservationSpecieFragment_recyclerView
    }

    private fun setupView() {
        searchBar.setListener(searchBarListener)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mushroomViewModel = ViewModelProviders.of(this).get(MushroomsViewModel::class.java)
        mushroomViewModel.state.observe(this, observer)


        bindViews()
        setupView()


        super.onViewCreated(view, savedInstanceState)
    }


}