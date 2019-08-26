package com.noque.svampeatlas.View.Fragments


import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ResourceCursorAdapter
import android.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Adapters.MyPageAdapter
import com.noque.svampeatlas.Model.AppError
import com.noque.svampeatlas.Model.Section
import com.noque.svampeatlas.Model.State

import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.BlankActivity
import com.noque.svampeatlas.View.Views.UserView
import com.noque.svampeatlas.ViewModel.SessionViewModel
import kotlinx.android.synthetic.main.activity_blank.*
import kotlinx.android.synthetic.main.fragment_my_page.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 *
 */
class MyPageFragment : Fragment() {

    // Views

    private var toolbar: androidx.appcompat.widget.Toolbar? = null
    private var recyclerView: RecyclerView? = null
    private var userView: UserView? = null

    // View models

    private lateinit var sessionViewModel: SessionViewModel

    // Adapters

    private val adapter by lazy {
        val adapter = MyPageAdapter()

        adapter.logOutButtonPressed = {
            sessionViewModel.logout()
        }

        adapter.notificationSelected = {
            val action = MyPageFragmentDirections.actionGlobalMushroomDetailsFragment(
                it.observationID,
                DetailsFragment.TakesSelection.NO,
                DetailsFragment.Type.OBSERVATION
            )

            findNavController().navigate(action)
        }

        adapter.observationSelected = {
            val action = MyPageFragmentDirections.actionGlobalMushroomDetailsFragment(
                it.id,
                DetailsFragment.TakesSelection.NO,
                DetailsFragment.Type.OBSERVATION
            )

            findNavController().navigate(action)
        }

        adapter.loadAdditionalDataSelected = { category, offset ->
            when (category) {
                MyPageAdapter.ItemType.Category.NOTIFICATIONS -> sessionViewModel.getAdditionalNotifications(
                    offset
                )
                MyPageAdapter.ItemType.Category.OBSERVATIONS -> sessionViewModel.getAdditionalObservations(
                    offset
                )
            }
        }

        adapter
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
        setupViewModels()
    }

    private fun initViews() {
        toolbar = mypageFragment_toolbar
        recyclerView = myPageFragment_recyclerView
        userView = myPageFragment_userView
    }

    private fun setupViews() {
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)

        myPageFragment_collapsingToolbarLayout.setExpandedTitleColor(Color.alpha(0))
        myPageFragment_collapsingToolbarLayout.setCollapsedTitleTextColor(
            ResourcesCompat.getColor(
                resources,
                R.color.colorWhite,
                null
            )
        )


        recyclerView?.apply {
            adapter = this@MyPageFragment.adapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    override fun onStart() {
        super.onStart()

    }


    private fun setupViewModels() {
        activity?.let {
            sessionViewModel = ViewModelProviders.of(it).get(SessionViewModel::class.java)

            sessionViewModel.user.observe(viewLifecycleOwner, Observer {
                toolbar?.title = it?.name
                if (it != null) userView?.configure(it) else userView?.configureAsGuest()
            })

            sessionViewModel.notificationsState.observe(viewLifecycleOwner, Observer { state ->

                val items = mutableListOf<MyPageAdapter.Item>()

                when (state) {
                    is State.Items -> {
                        if (state.items.first.count() == 0) {
                            items.add(
                                MyPageAdapter.Item(
                                    MyPageAdapter.ItemType.Error(
                                        AppError(
                                            resources.getString(R.string.myPageFragment_noNotificationsTitle),
                                            resources.getString(R.string.myPageFragment_noNotifications_message)
                                        )
                                    )
                                )
                            )
                        } else {
                            items.addAll(state.items.first.map {
                                MyPageAdapter.Item(
                                    MyPageAdapter.ItemType.Notification(
                                        it
                                    )
                                )
                            })
                            if (items.count() != state.items.second) items.add(
                                MyPageAdapter.Item(
                                    MyPageAdapter.ItemType.Reloader(
                                        MyPageAdapter.ItemType.Category.NOTIFICATIONS,
                                        items.lastIndex
                                    )
                                )
                            )
                        }

                        val section = Section("${state.items.second} notifikationer", items)
                        adapter.configureNotifications(listOf(section))
                    }
                }
            })

            sessionViewModel.observationsState.observe(viewLifecycleOwner, Observer { state ->

                val items = mutableListOf<MyPageAdapter.Item>()

                when (state) {
                    is State.Items -> {

                        items.addAll(state.items.first.map {
                            MyPageAdapter.Item(
                                MyPageAdapter.ItemType.Observation(
                                    it
                                )
                            )
                        })
                        if (items.count() != state.items.second) items.add(
                            MyPageAdapter.Item(
                                MyPageAdapter.ItemType.Reloader(
                                    MyPageAdapter.ItemType.Category.OBSERVATIONS,
                                    items.lastIndex
                                )
                            )
                        )

                        val section = Section("${state.items.second} observationer", items)
                        adapter.configureObservations(listOf(section))
                    }
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView = null
        userView = null
    }
}
