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
import com.noque.svampeatlas.Model.Mushroom

import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.Fragments.DetailsFragment
import com.noque.svampeatlas.ViewModel.DetailsViewModel
import com.noque.svampeatlas.ViewModel.MushroomsViewModel
import com.noque.svampeatlas.Model.State
import kotlinx.android.synthetic.main.fragment_list.*
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.noque.svampeatlas.View.Views.SearchBarDelegate


class MushroomFragment : Fragment() {

    private val searchBarDelegate = object: SearchBarDelegate {
        override fun newSearch(entry: String) {
           viewModel.search(entry)
        }

        override fun clearedSearchEntry() {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    private val onScrollListener = object: RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            if(!recyclerView.canScrollVertically(-1)) {
                fragmentList_searchBar.expand()
            } else if (dy > 0) {
                fragmentList_searchBar.collapse()
            }
        }
    }

    private val onRefreshListener = object: SwipeRefreshLayout.OnRefreshListener {
        override fun onRefresh() {
            viewModel.start()
            fragmentList_swipeRefreshLayout.isRefreshing = false
        }

    }

    private val stateObserver = object: Observer<State<List<Mushroom>>> {
        override fun onChanged(state: State<List<Mushroom>>) {
            fragmentList_backgroundView.reset()

            when (state) {
                is State.Loading -> {
                    adapter.updateData(listOf())
                    fragmentList_backgroundView.setLoading()
                }
                is State.Items -> {
                    adapter.updateData(state.items)
                }

                is State.Error -> {
                    fragmentList_backgroundView.setError(state.error)
                }
            }
        }
    }

    private val onTapSelectedListner = object: TabLayout.OnTabSelectedListener {
        override fun onTabReselected(p0: TabLayout.Tab?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onTabUnselected(p0: TabLayout.Tab?) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onTabSelected(tab: TabLayout.Tab) {
        }

    }

    lateinit var viewModel: MushroomsViewModel

    private val adapter: MushroomListAdapter by lazy {
        val adapter = MushroomListAdapter()
        adapter.setOnClickListener { mushroom ->
            val action = DetailsFragmentDirections.actionGlobalDetailsFragment()
            findNavController().navigate(action)

            activity?.let {
                val detailViewModel = ViewModelProviders.of(it).get(DetailsViewModel::class.java)
                detailViewModel.select(mushroom)
            }
        }

        adapter
    }

//    private val listAdapter = MushroomListAdapter(arrayListOf(), { mushroom ->
//                        val action = DetailsFragmentDirections.actionGlobalDetailsFragment()
////                            .actionGlobalDetailsFragment(mushroom.images.toTypedArray())
//
//            activity?.let {
//                val detailViewModel = ViewModelProviders.of(it).get(DetailsViewModel::class.java)
//                detailViewModel.select(mushroom)
//                findNavController().navigate(action)
////                TODO("FIND OUT HOW TO DESTROY VIEWMODEL WHEN DETAILSSCREEN IS DESTROYED")
//            }
//    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MushroomsViewModel::class.java)
        setupView()
    }

    private fun setupView() {
        mushroom_recyclerview.addOnScrollListener(onScrollListener)
        fragmentList_searchBar.setListener(searchBarDelegate)
        fragmentList_swipeRefreshLayout.setOnRefreshListener(onRefreshListener)
//        fragmentList_TabLayout.addOnTabSelectedListener()


        mushroom_recyclerview.adapter = adapter
        mushroom_recyclerview.layoutManager = LinearLayoutManager(context)

        viewModel.state.observe(this, stateObserver)
        viewModel.start()


    }
}