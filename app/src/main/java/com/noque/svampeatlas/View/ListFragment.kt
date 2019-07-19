package com.noque.svampeatlas.View


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.noque.svampeatlas.Adapters.MushroomListAdapter
import com.noque.svampeatlas.Model.Mushroom

import com.noque.svampeatlas.R
import com.noque.svampeatlas.ViewModel.DetailsViewModel
import com.noque.svampeatlas.ViewModel.MushroomsViewModel
import com.noque.svampeatlas.ViewModel.State
import kotlinx.android.synthetic.main.fragment_list.*

class ListFragment : Fragment() {

    lateinit var viewModel: MushroomsViewModel
    private val listAdapter = MushroomListAdapter(arrayListOf(), { mushroom ->
                        val action = DetailsFragmentDirections.actionGlobalDetailsFragment()
//                            .actionGlobalDetailsFragment(mushroom.images.toTypedArray())

            activity?.let {
                val detailViewModel = ViewModelProviders.of(it).get(DetailsViewModel::class.java)
                detailViewModel.select(mushroom)
                findNavController().navigate(action)
//                TODO("FIND OUT HOW TO DESTROY VIEWMODEL WHEN DETAILSSCREEN IS DESTROYED")
            }
    })

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MushroomsViewModel::class.java)
        configureView()
    }

    private fun configureView() {
        viewModel.state.observe(this, Observer {
            setState(it)
        })
        viewModel.start()


        mushroom_recyclerview.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = listAdapter
        }

        swiperefreshlayout_list.setOnRefreshListener {
            viewModel.start()
            swiperefreshlayout_list.isRefreshing = false

        }
    }

    private fun setState(state: State<List<Mushroom>>) {
        when (state) {
            is State.Loading -> {
                mushroom_progressBar_loading.visibility = View.VISIBLE
                mushroom_recyclerview.visibility = View.GONE
            }
            is State.Items -> {
                mushroom_recyclerview.visibility = View.VISIBLE
                mushroom_progressBar_loading.visibility = View.GONE
                listAdapter.updateData(state.items)
            }
        }
    }
}