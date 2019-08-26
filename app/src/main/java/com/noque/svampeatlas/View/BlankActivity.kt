package com.noque.svampeatlas.View

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.se.omapi.Session
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.noque.svampeatlas.Model.State
import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.Fragments.*
import com.noque.svampeatlas.View.Views.UserView
import com.noque.svampeatlas.ViewModel.SessionViewModel
import kotlinx.android.synthetic.main.activity_blank.*
import kotlinx.android.synthetic.main.navigation_header.view.*
import java.io.File


class BlankActivity : AppCompatActivity() {

    companion object {
        val TAG = "BlankActivity"
        val KEY_IS_LOGGED_IN = "IsLoggedIn"

        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }

            return if (mediaDir != null && mediaDir.exists()) mediaDir else appContext.filesDir
        }
    }


    // Objects
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private var isLoggedIn: Boolean? = null


    // Views
    private lateinit var navigationView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var userView: UserView

    // View models

    private lateinit var sessionViewModel: SessionViewModel

    // Listeners

    private val onDestinationChangedListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
//            toolbar.setupWithNavController(navController, appBarConfiguration)
//            setSupportActionBar(toolbar)
//            drawerLayout.fitsSystemWindows = true

//            when (destination.id) {
//                R.id.onboardingFragment -> {
//                    toolbar.visibility = View.GONE
//                }
//
//                R.id.mushroomDetailsFragment -> {
//                    toolbar.visibility = View.GONE
//                }
//
//                R.id.myPageFragment -> {
//                    toolbar.visibility = View.GONE
//                }
//
//                R.id.cameraFragment -> {
//                    hideSystemBars()
//                    drawerLayout.fitsSystemWindows = false
//                    toolbar.visibility = View.GONE
//                }
//
//
//                else -> {
//                    toolbar.visibility = View.VISIBLE
//                    showSystemBars()
//                }
//            }
        }

    private val onNavigationItemSelectedListener = NavigationView.OnNavigationItemSelectedListener {
        drawerLayout.closeDrawer(navigationView, true)
        navigationView.setCheckedItem(it)

//        if (it.itemId != navController.currentDestination?.id) {
//            when (it.itemId) {
//                R.id.mushroomFragment -> {
//                    navController.navigate(MushroomFragmentDirections.actionGlobalMushroomFragment())
//                }
//
//                R.id.addObservationFragment -> {
//                    navController.navigate(AddObservationFragmentDirections.actionGlobalAddObservationFragment())
//                }
//
//                R.id.loginFragment -> {
//                    navController.navigate(LoginFragmentDirections.actionGlobalLoginFragment())
//                }
//
//                R.id.nearbyFragment -> {
//                    navController.navigate(NearbyFragmentDirections.actionGlobalNearbyFragment())
//                }
//
//                R.id.myPageFragment -> {
//                    navController.navigate(MyPageFragmentDirections.actionGlobalMyPageFragment())
//                }
//            }
//        }
        true
    }

//    override fun onWindowFocusChanged(hasFocus: Boolean) {
//        if (hasFocus && navController.currentDestination?.id == R.id.cameraFragment) {
//            hideSystemBars()
//        }
//
//        super.onWindowFocusChanged(hasFocus)
//    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isLoggedIn = savedInstanceState?.getBoolean(KEY_IS_LOGGED_IN)
        setContentView(R.layout.activity_blank)

        initViews()
        setupView()
        setupViewModels()
    }

    private fun initViews() {

        drawerLayout = blankActitivy_drawerLayout
        navController = findNavController(R.id.blankActivity_navHostFragment)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.myPageFragment,
                R.id.mushroomFragment,
                R.id.nearbyFragment,
                R.id.addObservationFragment,
                R.id.loginFragment
            ), drawerLayout
        )
        navigationView = blankActivity_navigationView
        userView = navigationView.getHeaderView(0).navigationHeader_userView
    }

    private fun setupView() {
        window.statusBarColor = Color.TRANSPARENT


//        setSupportActionBar(toolbar)
//        setupActionBarWithNavController(navController, appBarConfiguration)
//        navController.addOnDestinationChangedListener(onDestinationChangedListener)

        navigationView.itemIconTintList = null
        navigationView.setupWithNavController(navController)
//        navigationView.setNavigationItemSelectedListener(onNavigationItemSelectedListener)
    }

    private fun setupViewModels() {
        sessionViewModel = ViewModelProviders.of(this).get(SessionViewModel::class.java)

        sessionViewModel.loggedInState.observe(this, Observer {

            when (it) {
                is State.Items -> {
                    navigationView.menu.clear()
                    if (it.items) navigationView.inflateMenu(R.menu.menu_logged_in) else navigationView.inflateMenu(
                        R.menu.menu_logged_out
                    )

                    if (it.items != isLoggedIn && it.items) {
                        val newGraph = navController.graph
                        newGraph.startDestination = R.id.myPageFragment
                        navigationView.setCheckedItem(R.id.myPageFragment)
                        navController.graph = newGraph
//                        toolbar.setupWithNavController(navController, appBarConfiguration)
                    } else if (it.items != isLoggedIn && !it.items) {
                        val newGraph = navController.graph
                        newGraph.startDestination = R.id.mushroomFragment
                        navigationView.setCheckedItem(R.id.mushroomFragment)
                        navController.graph = newGraph
                    }
                    isLoggedIn = it.items
                }
            }
        })

        sessionViewModel.user.observe(this, Observer {
            if (it != null) userView.configure(it) else userView.configureAsGuest()
        })
    }

    fun hideSystemBars() {
        val flags = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                )

        window.decorView.systemUiVisibility = flags
    }

    fun showSystemBars() {
            Log.d(TAG, View.SYSTEM_UI_FLAG_VISIBLE.toString())


//        val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                or  View.SYSTEM_UI_FLAG_FULLSCREEN
//                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
//        navigationView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        isLoggedIn?.let {
            outState.putBoolean(KEY_IS_LOGGED_IN, it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
