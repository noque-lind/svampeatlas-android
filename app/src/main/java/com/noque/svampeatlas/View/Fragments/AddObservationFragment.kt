package com.noque.svampeatlas.View.Fragments

import android.database.DataSetObserver
import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.tabs.TabLayout
import com.noque.svampeatlas.Adapters.AddObservationAdapters.InformationAdapter

import com.noque.svampeatlas.R
import com.noque.svampeatlas.Services.LocationService
import com.noque.svampeatlas.Services.LocationServiceDelegate
import com.noque.svampeatlas.ViewModel.MushroomsViewModel
import com.noque.svampeatlas.ViewModel.NewObservationViewModel
import kotlinx.android.synthetic.main.fragment_add_observation.*
import kotlinx.android.synthetic.main.fragment_nearby.*
import java.lang.Exception

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [AddObservationFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [AddObservationFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class AddObservationFragment : Fragment() {

    enum class Categories(val value: String) {
        SPECIES("Art"),
        DETAILS("Detajler"),
        LOCALITY("Lokation")
    }

    private val categories = Categories.values()

    private var param1: String? = null
    private var param2: String? = null
    private var listener: OnFragmentInteractionListener? = null

    private var locationService: LocationService? = null
    lateinit var viewModel: NewObservationViewModel
    lateinit var adapter: InformationAdapter


    private val locationServiceDelegate = object: LocationServiceDelegate {
        override fun locationRetrieved(location: Location) {
            viewModel.getLocalities(LatLng(location.latitude, location.longitude))
        }

        override fun locationRetrievalError(exception: Exception) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_observation, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configure()
        setupView()

        locationService?.start()
    }

    private fun configure() {
        activity?.let {
            viewModel = ViewModelProviders.of(it).get(NewObservationViewModel::class.java)
        }



        if (context != null && activity != null) {
            locationService = LocationService(context!!, activity!!)
            locationService?.setListener(locationServiceDelegate)
        }


        fragmentManager?.let {
            adapter = InformationAdapter(categories, it, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT)
            addObservationFragment_viewPager.adapter = adapter
        }
    }

    private fun setupView() {

        categories.forEach {
            val tab = addObservationFragment_tabLayout.newTab()
            tab.text = it.value
            tab.tag = it
            addObservationFragment_tabLayout.addTab(tab)
        }


        addObservationFragment_tabLayout.addOnTabSelectedListener(onTapSelectedListner)

//        tabLayout.addOnTabSelectedListener(onTapSelectedListner)

    }

    private val onTapSelectedListner = object: TabLayout.OnTabSelectedListener {
        override fun onTabReselected(p0: TabLayout.Tab?) {}
        override fun onTabUnselected(p0: TabLayout.Tab?) {}

        override fun onTabSelected(tab: TabLayout.Tab) {
            (tab.tag as? Categories)?.let {
                addObservationFragment_viewPager.setCurrentItem(it.ordinal, true)
            }
        }
    }


    override fun onDetach() {
        super.onDetach()
        listener = null
    }



















    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */







    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AddObservationFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AddObservationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}
