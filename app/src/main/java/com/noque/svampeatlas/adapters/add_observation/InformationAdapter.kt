package com.noque.svampeatlas.adapters.add_observation

import android.content.Context
import androidx.fragment.app.*
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.noque.svampeatlas.R
import com.noque.svampeatlas.fragments.AddObservationFragment
import com.noque.svampeatlas.fragments.add_observation.DetailsFragment
import com.noque.svampeatlas.fragments.add_observation.LocalityFragment
import com.noque.svampeatlas.fragments.add_observation.SpeciesFragment


class InformationAdapter(val context: Context?, val categories: Array<AddObservationFragment.Category>, fm: FragmentManager, behavior: Int) : FragmentPagerAdapter(fm, behavior) {


    override fun getItem(position: Int): Fragment {
        return when (categories[position]) {
            AddObservationFragment.Category.SPECIES -> SpeciesFragment()
            AddObservationFragment.Category.DETAILS -> DetailsFragment()
            AddObservationFragment.Category.LOCALITY -> LocalityFragment()
        }
    }

    override fun getCount(): Int {
        return categories.count()
    }


    override fun getPageTitle(position: Int): CharSequence? {
        return when (categories[position]) {
            AddObservationFragment.Category.DETAILS -> context?.resources?.getText(R.string.addObservationVC_observationCategories_details)
            AddObservationFragment.Category.LOCALITY -> context?.resources?.getText(R.string.addObservationVC_observationCategories_location)
            AddObservationFragment.Category.SPECIES -> context?.resources?.getText(R.string.addObservationVC_observationCategories_species)
        }
    }
}