package com.noque.svampeatlas.views
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.google.android.material.navigation.NavigationView
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.activity_blank.*
import kotlinx.android.synthetic.main.navigation_header.view.*
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.util.Log
import android.view.View.*
import androidx.navigation.*
import androidx.navigation.fragment.findNavController
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.ktx.Firebase
import com.noque.svampeatlas.fragments.TermsFragment
import com.noque.svampeatlas.services.Analytics
import com.noque.svampeatlas.services.FileManager
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.view_models.Session
import java.lang.Exception


class BlankActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "BlankActivity"
        const val KEY_IS_LOGGED_IN = "IsLoggedIn"
    }


    // Objects
    private lateinit var navController: NavController
    private var isLoggedIn: Boolean? = null

    // Views
    private lateinit var navigationView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var userView: UserView

    // Listeners

    private val onDestinationChangedListener by lazy {
        NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.id != R.id.addObservationFragment || destination.id != R.id.cameraFragment) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            }
            navigationView.setCheckedItem(destination.id)
        }
    }

    private val onNavigationItemSelectedListener by lazy {
        NavigationView.OnNavigationItemSelectedListener {
            var setCheckedItem = true
            var closeDrawer = true
            var destinationID: Int? = it.itemId

                when (it.itemId) {
                    R.id.facebook -> {
                        setCheckedItem = false
                        closeDrawer = false
                        destinationID = null

                        val intent = try {
                            packageManager.getPackageInfo("com.facebook.katana", 0)
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("fb://facewebmodal/f?href=https://www.facebook.com/groups/svampeatlas")
                            )
                        } catch (e: Exception) {
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://www.facebook.com/groups/svampeatlas/")
                            )
                        }

                        try {
                            startActivity(intent)
                        } catch (e: Exception) {
                            Log.d(TAG, e.toString())
                        }
                    }
                }

            if (closeDrawer) drawerLayout.closeDrawer(navigationView, true)
            if (setCheckedItem) navigationView.setCheckedItem(it.itemId)
            if (destinationID != null && destinationID != navController.currentDestination?.id) {
                destinationID?.let {
                    navController.navigate(
                        it,
                        null,
                        NavOptions.Builder().setPopUpTo(navController.graph.startDestination, false).build()
                    )
                }
            }
            false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        Analytics.setInstance(FirebaseAnalytics.getInstance(this))
        super.onCreate(savedInstanceState)
        isLoggedIn = savedInstanceState?.getBoolean(KEY_IS_LOGGED_IN)
        setContentView(R.layout.activity_blank)

        initViews()
        setupView()
        setupViewModels()
    }

    private fun initViews() {
        drawerLayout = blankActitivy_drawerLayout
        navController = supportFragmentManager.findFragmentById(R.id.blankActivity_navHostFragment)!!.findNavController()
        navigationView = blankActivity_navigationView
        userView = navigationView.getHeaderView(0).navigationHeader_userView
    }

    private fun setupView() {
        window.statusBarColor = Color.TRANSPARENT
        navigationView.itemIconTintList = null
        navController.addOnDestinationChangedListener(onDestinationChangedListener)
        navigationView.setNavigationItemSelectedListener(onNavigationItemSelectedListener)
        if (!SharedPreferences.hasSeenWhatsNew) {
            val dialog = TermsFragment()
            dialog.arguments = Bundle().apply { putSerializable(TermsFragment.KEY_TYPE, TermsFragment.Type.WHATSNEW) }
            dialog.show(supportFragmentManager, null)
            SharedPreferences.lastDownloadOfTaxon = null
        }
    }

    private fun setupViewModels() {
        Session.loggedInState.observe(this, Observer {

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

        Session.user.observe(this, Observer {
            if (it != null) userView.configure(it) else userView.configureAsGuest()
        })
    }

    fun hideSystemBars() {
        val flags = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                                SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_STABLE or SYSTEM_UI_FLAG_LOW_PROFILE
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
        val sharedSet = mutableSetOf(R.id.loginFragment, R.id.myPageFragment, R.id.notesFragment, R.id.mushroomFragment, R.id.nearbyFragment, R.id.cameraFragment, R.id.settingsFragment, R.id.aboutFragment)
       if (navController.currentDestination?.id == R.id.addObservationFragment && (navController.previousBackStackEntry?.destination?.id == R.id.mushroomDetailsFragment || navController.previousBackStackEntry?.destination?.id == R.id.notesFragment)) {
           return navController.navigateUp(AppBarConfiguration(sharedSet, drawerLayout)) || super.onSupportNavigateUp()
       } else  {
           sharedSet.add(R.id.addObservationFragment)
           return navController.navigateUp(AppBarConfiguration(sharedSet, drawerLayout)) || super.onSupportNavigateUp()
       }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        if (isFinishing) {
            // Note: this happens only when application is closed by exiting it probably, meaning that potentially a lot of temp images could end up be saved unintentionally
            FileManager.clearTemporaryFiles()
        }

        super.onPause()
    }
}
