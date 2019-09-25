package com.noque.svampeatlas.fragments


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.noque.svampeatlas.adapters.MyPageAdapter

import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.views.BlankActivity
import com.noque.svampeatlas.views.ProfileImageView
import com.noque.svampeatlas.view_models.SessionViewModel
import kotlinx.android.synthetic.main.fragment_my_page.*

class MyPageFragment : Fragment() {

    companion object {
        val TAG = "MyPageFragment"
    }

    // Views

    private var toolbar: androidx.appcompat.widget.Toolbar? = null
    private var recyclerView: RecyclerView? = null
    private var userView: ProfileImageView? = null
    private var collapsingToolbar: CollapsingToolbarLayout? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null


    // View models

    private val sessionViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(SessionViewModel::class.java)
    }

    // Adapters


    private val adapter by lazy {
        val adapter = MyPageAdapter()

        adapter.setListener(object: MyPageAdapter.Listener {
            override fun observationSelected(observation: Observation) {
                val action = MyPageFragmentDirections.actionGlobalMushroomDetailsFragment(
                    observation.id,
                    DetailsFragment.TakesSelection.NO,
                    DetailsFragment.Type.OBSERVATION,
                    null,
                    null
                )

                findNavController().navigate(action)
            }

            override fun getAdditionalData(category: MyPageAdapter.Item.Category, atOffset: Int) {
                when (category) {
                    MyPageAdapter.Item.Category.NOTIFICATIONS -> sessionViewModel.getAdditionalNotifications(
                        atOffset
                    )
                    MyPageAdapter.Item.Category.OBSERVATIONS -> sessionViewModel.getAdditionalObservations(
                        atOffset
                    )
                }
            }

            override fun notificationSelected(notification: Notification) {
                val action = MyPageFragmentDirections.actionGlobalMushroomDetailsFragment(
                    notification.observationID,
                    DetailsFragment.TakesSelection.NO,
                    DetailsFragment.Type.OBSERVATION,
                    null,
                    null
                )

                findNavController().navigate(action)
            }

            override fun logoutButtonSelected() {
                sessionViewModel.logout()
            }

        })

        adapter
    }

    // Listeners

    private val onRefreshListener = SwipeRefreshLayout.OnRefreshListener {
        sessionViewModel.reloadData(true)
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

        sessionViewModel.reloadData(false)
    }

    private fun initViews() {
        toolbar = mypageFragment_toolbar
        recyclerView = myPageFragment_recyclerView
        userView = myPageFragment_profileImageView
        collapsingToolbar = myPageFragment_collapsingToolbarLayout
        swipeRefreshLayout = myPageFragment_swipeRefreshLayout
    }

    private fun setupViews() {
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
        collapsingToolbar?.setCollapsedTitleTextColor(ResourcesCompat.getColor(resources, R.color.colorWhite, null))
        collapsingToolbar?.setExpandedTitleColor(ResourcesCompat.getColor(resources, R.color.colorWhite, null))
        swipeRefreshLayout?.setOnRefreshListener(onRefreshListener)

        recyclerView?.apply {
            adapter = this@MyPageFragment.adapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    private fun setupViewModels() {
            sessionViewModel.user.observe(viewLifecycleOwner, Observer {
                collapsingToolbar?.title = it?.name
                if (it != null) userView?.configure(it.initials, it.imageURL, DataService.ImageSize.FULL)
            })

            sessionViewModel.notificationsState.observe(viewLifecycleOwner, Observer { state ->
                when (state) {
                    is State.Loading -> {
                        swipeRefreshLayout?.isRefreshing = true
                    }

                    is State.Items -> {
                        if (state.items.first.count() == 0) {
                            adapter.configureNotificationsState(State.Error(AppError(resources.getString(R.string.myPageFragment_noNotificationsTitle),
                                resources.getString(R.string.myPageFragment_noNotifications_message))), getString(R.string.myPageFragment_notificationsHeader))
                        } else {
                            val items: MutableList<MyPageAdapter.Item> = state.items.first.map { MyPageAdapter.Item.Notification(it) }.toMutableList()
                            if (items.count() != state.items.second) items.add(MyPageAdapter.Item.LoadMore(MyPageAdapter.Item.Category.NOTIFICATIONS, items.lastIndex))
                            adapter.configureNotificationsState(State.Items(items), "${state.items.second} ${getText(R.string.myPageFragment_notificationsHeader)}")
                        }
                    }

                    is State.Error -> {
                        adapter.configureNotificationsState(State.Error(state.error))
                    }
                }

                evaluateIfFinishedLoading()
            })

            sessionViewModel.observationsState.observe(viewLifecycleOwner, Observer { state ->
                when (state) {
                    is State.Loading -> {
                        swipeRefreshLayout?.isRefreshing = true
                    }

                    is State.Items -> {
                        if (state.items.first.count() == 0) {
                            adapter.configureObservationsState(State.Error(AppError(
                                resources.getString(R.string.myPageFragment_noObservations_title),
                                resources.getString(R.string.myPageFragment_noObservations_message)
                            )), getString(R.string.myPageFragment_observationsHeader))
                        } else {
                            val items: MutableList<MyPageAdapter.Item> = state.items.first.map { MyPageAdapter.Item.Observation(it) }.toMutableList()
                            if (items.count() != state.items.second) items.add(MyPageAdapter.Item.LoadMore(MyPageAdapter.Item.Category.OBSERVATIONS, items.lastIndex))
                            adapter.configureObservationsState(State.Items(items), "\${state.items.second} ${getText(
                                R.string.myPageFragment_observationsHeader
                            )}")
                        }
                    }

                    is State.Error -> {
                        adapter.configureObservationsState(State.Error(state.error))
                    }
                }

                evaluateIfFinishedLoading()
            })
    }

    private fun evaluateIfFinishedLoading() {
        if (sessionViewModel.observationsState.value !is State.Loading && sessionViewModel.notificationsState.value !is State.Loading) {
            swipeRefreshLayout?.isRefreshing = false
        }
    }

    override fun onDestroyView() {
        recyclerView?.adapter = null
        recyclerView = null
        userView = null
        swipeRefreshLayout = null
        toolbar = null
        collapsingToolbar = null
        super.onDestroyView()
    }
}
