package com.noque.svampeatlas.fragments


import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.noque.svampeatlas.adapters.MyPageAdapter

import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.view_models.Session
import com.noque.svampeatlas.views.BlankActivity
import com.noque.svampeatlas.views.ProfileImageView
import kotlinx.android.synthetic.main.fragment_my_page.*

class MyPageFragment : Fragment() {

    companion object {
        val TAG = "MyPageFragment"
    }

    // Views

    private var toolbar by autoCleared<Toolbar>()
    private var recyclerView by autoCleared<RecyclerView> { it?.adapter = null }
    private var userView by autoCleared<ProfileImageView>()
    private var collapsingToolbar by autoCleared<CollapsingToolbarLayout>()
    private var swipeRefreshLayout by autoCleared<SwipeRefreshLayout>()


    // Adapters


    private val adapter by lazy {
        val adapter = MyPageAdapter()

        adapter.setListener(object: MyPageAdapter.Listener {
            override fun observationSelected(observation: Observation) {
                val action = MyPageFragmentDirections.actionGlobalMushroomDetailsFragment(
                    observation.id,
                    DetailsFragment.TakesSelection.NO,
                    DetailsFragment.Type.OBSERVATIONWITHSPECIES,
                    null,
                    null
                )

                findNavController().navigate(action)
            }

            override fun getAdditionalData(category: MyPageAdapter.Item.Category, atOffset: Int) {
                when (category) {
                    MyPageAdapter.Item.Category.NOTIFICATIONS -> Session.getAdditionalNotifications(
                        atOffset
                    )
                    MyPageAdapter.Item.Category.OBSERVATIONS -> Session.getAdditionalObservations(
                        atOffset
                    )
                }
            }

            override fun notificationSelected(notification: Notification) {
                Session.markNotificationAsRead(notification)
                val action = MyPageFragmentDirections.actionGlobalMushroomDetailsFragment(
                    notification.observationID,
                    DetailsFragment.TakesSelection.NO,
                    DetailsFragment.Type.OBSERVATIONWITHSPECIES,
                    null,
                    null
                )

                findNavController().navigate(action)
            }

            override fun logoutButtonSelected() {
                Session.logout()
            }

        })

        adapter
    }

    // Listeners

    private val onRefreshListener = SwipeRefreshLayout.OnRefreshListener {
        Session.reloadData(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_my_page, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.my_page_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_myPageFragment_logOut -> Session.logout()
        }
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = mypageFragment_toolbar
        recyclerView = myPageFragment_recyclerView
        userView = myPageFragment_profileImageView
        collapsingToolbar = myPageFragment_collapsingToolbarLayout
        swipeRefreshLayout = myPageFragment_swipeRefreshLayout
        setupViews()
        setupViewModels()
        Session.reloadData(false)
    }

    private fun setupViews() {
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
        collapsingToolbar.setCollapsedTitleTextColor(ResourcesCompat.getColor(resources, R.color.colorWhite, null))
        collapsingToolbar.setExpandedTitleColor(ResourcesCompat.getColor(resources, R.color.colorWhite, null))
        swipeRefreshLayout.setOnRefreshListener(onRefreshListener)
        recyclerView.apply {
            adapter = this@MyPageFragment.adapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    private fun setupViewModels() {
            Session.user.observe(viewLifecycleOwner, Observer {
                collapsingToolbar.title = it?.name
                if (it != null) userView.configure(it.initials, it.imageURL, DataService.ImageSize.FULL)
            })

            Session.notificationsState.observe(viewLifecycleOwner, Observer { state ->
                when (state) {
                    is State.Loading -> {
                        swipeRefreshLayout.isRefreshing = true
                    }

                    is State.Items -> {
                        if (state.items.first.count() == 0) {
                            adapter.configureNotificationsState(State.Error(AppError(resources.getString(R.string.error_session_noNotifications_title),
                                resources.getString(R.string.error_session_noNotifications_message), null)), getString(R.string.myPageScrollView_notificationsHeader))
                        } else {
                            val items: MutableList<MyPageAdapter.Item> = state.items.first.map { MyPageAdapter.Item.Notification(it) }.toMutableList()
                            if (items.count() != state.items.second) items.add(MyPageAdapter.Item.LoadMore(MyPageAdapter.Item.Category.NOTIFICATIONS, items.lastIndex))
                            adapter.configureNotificationsState(State.Items(items), "${state.items.second} ${getText(R.string.myPageScrollView_notificationsHeader)}")
                        }
                    }

                    is State.Error -> {
                        adapter.configureNotificationsState(State.Error(state.error), getString(R.string.myPageScrollView_notificationsHeader))
                    }
                }

                evaluateIfFinishedLoading()
            })

            Session.observationsState.observe(viewLifecycleOwner, Observer { state ->
                when (state) {
                    is State.Loading -> {
                        swipeRefreshLayout.isRefreshing = true
                    }

                    is State.Items -> {
                        if (state.items.first.count() == 0) {
                            adapter.configureObservationsState(State.Error(AppError(
                                resources.getString(R.string.error_session_noObservations_title),
                                resources.getString(R.string.error_session_noObservations_message), null
                            )), getString(R.string.myPageScrollView_observationsHeader))
                        } else {
                            val items: MutableList<MyPageAdapter.Item> = state.items.first.map { MyPageAdapter.Item.Observation(it) }.toMutableList()
                            if (items.count() != state.items.second) items.add(MyPageAdapter.Item.LoadMore(MyPageAdapter.Item.Category.OBSERVATIONS, items.lastIndex))
                            adapter.configureObservationsState(State.Items(items), "${state.items.second} ${getText(
                                R.string.myPageScrollView_observationsHeader
                            )}")
                        }
                    }

                    is State.Error -> {
                        adapter.configureObservationsState(State.Error(state.error), getString(R.string.myPageScrollView_observationsHeader))
                    }
                }

                evaluateIfFinishedLoading()
            })
    }

    private fun evaluateIfFinishedLoading() {
        if (Session.observationsState.value !is State.Loading && Session.notificationsState.value !is State.Loading) {
            swipeRefreshLayout.isRefreshing = false
        }
    }
}
