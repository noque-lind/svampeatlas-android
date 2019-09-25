package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.R
import com.noque.svampeatlas.view_models.SessionViewModel
import kotlinx.android.synthetic.main.activity_blank.*
import kotlinx.android.synthetic.main.navigation_header.view.*
import java.io.File
import androidx.core.content.ContextCompat.startActivity
import android.content.Intent
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.net.Uri
import android.util.Log
import java.lang.Exception


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

        fun getCacheDir(context: Context): File {
            val appContext = context.applicationContext
            val cacheDir = context.externalCacheDirs.firstOrNull()
            return if (cacheDir != null && cacheDir.exists()) cacheDir else appContext.cacheDir
        }
    }


    // Objects
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
        var setCheckedItem = true
        var closeDrawer = true
        var id: Int? = it.itemId


        if (it.itemId != navController.currentDestination?.id) {
            when (it.itemId) {
                R.id.facebook -> {
                    setCheckedItem = false
                    closeDrawer = false
                    id = null

                    val intent = try {
                        packageManager.getPackageInfo("com.facebook.katana", 0)
                        Intent(Intent.ACTION_VIEW, Uri.parse("fb://facewebmodal/f?href=https://www.facebook.com/groups/svampeatlas"))
                    } catch (e: Exception) {
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/groups/svampeatlas/"))
                    }

                    try {
                        startActivity(intent)
                    } catch (e: Exception) { Log.d(TAG, e.toString()) }
                }
            }
        }


        if (closeDrawer) drawerLayout.closeDrawer(navigationView, true)
        if (setCheckedItem) navigationView.setCheckedItem(it.itemId)
        if (id != null)  navController.navigate(id, null, NavOptions.Builder().setPopUpTo(navController.graph.startDestination, false).build())
        false
    }


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
        navigationView = blankActivity_navigationView
        userView = navigationView.getHeaderView(0).navigationHeader_userView
    }

    private fun setupView() {
        window.statusBarColor = Color.TRANSPARENT
        navigationView.itemIconTintList = null
//        navigationView.setupWithNavController(navController)
        navigationView.setNavigationItemSelectedListener(onNavigationItemSelectedListener)
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
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        isLoggedIn?.let {
            outState.putBoolean(KEY_IS_LOGGED_IN, it)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(AppBarConfiguration(setOf(R.id.loginFragment, R.id.myPageFragment, R.id.addObservationFragment, R.id.mushroomFragment, R.id.nearbyFragment, R.id.cameraFragment), drawerLayout)) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
