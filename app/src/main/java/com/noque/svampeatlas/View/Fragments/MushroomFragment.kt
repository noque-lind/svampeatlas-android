package com.noque.svampeatlas.View.Fragments


import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Adapters.MushroomListAdapter

import com.noque.svampeatlas.R
import com.noque.svampeatlas.ViewModel.MushroomsViewModel
import com.noque.svampeatlas.Model.State
import kotlinx.android.synthetic.main.fragment_mushroom.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.noque.svampeatlas.View.BackgroundView
import com.noque.svampeatlas.View.BlankActivity
import com.noque.svampeatlas.View.Views.SearchBarListener
import com.noque.svampeatlas.View.Views.SearchBarView


class MushroomFragment : Fragment() {

    companion object {
        val TAG = "MushroomFragment"
    }

    enum class Category {
        FAVORITES,
        SPECIES;

        companion object {
           val values = Category.values()
        }
    }

    // Views

    private var recyclerView: RecyclerView? = null
    private var backgroundView: BackgroundView? = null
    private var searchBarView: SearchBarView? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var tabLayout: TabLayout? = null

    // View models

    private lateinit var mushroomsViewModel: MushroomsViewModel

    // Adapters

    private val mushroomListAdapter: MushroomListAdapter by lazy {
        val adapter = MushroomListAdapter()

        adapter.setOnClickListener { mushroom ->
            val action = MushroomFragmentDirections.actionGlobalMushroomDetailsFragment(mushroom.id, DetailsFragment.TakesSelection.NO, DetailsFragment.Type.SPECIES)
            findNavController().navigate(action)
        }

        adapter
    }

    // Listeners

    private val searchBarListener = object: SearchBarListener {
        override fun newSearch(entry: String) {
           mushroomsViewModel.search(entry)
        }

        override fun clearedSearchEntry() {
        }

    }

    private val onScrollListener = object: RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if(!recyclerView.canScrollVertically(-1)) {
                searchBarView?.expand()
            } else if (dy > 0) {
                searchBarView?.collapse()
            }
        }
    }

    private val onRefreshListener = object: SwipeRefreshLayout.OnRefreshListener {
        override fun onRefresh() {
            mushroomsViewModel.start()
            swipeRefreshLayout?.isRefreshing = false
        }

    }

    private val onTapSelectedListener = object: TabLayout.OnTabSelectedListener {
        override fun onTabReselected(p0: TabLayout.Tab?) {}

        override fun onTabUnselected(p0: TabLayout.Tab?) {}

        override fun onTabSelected(tab: TabLayout.Tab) {

        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mushroom, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupView()
        setupViewModels()
    }


    private fun initViews() {
        recyclerView = mushroomFragment_recyclerView
        backgroundView = mushroomFragment_backgroundView
        searchBarView = mushroomFragment_searchBarView
        swipeRefreshLayout = mushroomFragment_swipeRefreshLayout
        tabLayout = mushroomFragment_tabLayout
    }

    private fun setupView() {
        (requireActivity() as BlankActivity).setSupportActionBar(mushroomFragment_toolbar)


        tabLayout?.apply {
            Category.values.forEach {
                val tab = this.newTab()

                when (it) {
                    Category.FAVORITES -> {tab.text = resources.getText(R.string.mushroomCategory_favorites)}
                    Category.SPECIES -> {tab.text = resources.getText(R.string.mushroomCategory_species)}
                }

                tab.tag = it
                this.addTab(tab)
            }
            this.addOnTabSelectedListener(onTapSelectedListener)
        }

        recyclerView?.apply {
            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = RecyclerView.VERTICAL
            this.layoutManager = layoutManager
            this.addOnScrollListener(onScrollListener)
            this.adapter = mushroomListAdapter
        }

        swipeRefreshLayout?.apply {
            setOnRefreshListener(onRefreshListener)
        }

        searchBarView?.apply {
            setListener(searchBarListener)
        }
    }

    private fun setupViewModels() {
        activity?.let {
            mushroomsViewModel = ViewModelProviders.of(it).get(MushroomsViewModel::class.java)
            mushroomsViewModel.mushroomsState.observe(viewLifecycleOwner, Observer {
                backgroundView?.reset()

                when (it) {
                    is State.Loading -> {
                        mushroomListAdapter.updateData(listOf())
                        backgroundView?.setLoading()
                    }
                    is State.Items -> {
                        mushroomListAdapter.updateData(it.items)
                    }

                    is State.Error -> {
                        backgroundView?.setError(it.error)
                    }
                }
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
        backgroundView = null
        searchBarView = null
        swipeRefreshLayout = null
        swipeRefreshLayout = null
        tabLayout = null
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