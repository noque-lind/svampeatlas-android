package com.noque.svampeatlas.Adapters.AddObservationAdapters

import android.content.Context
import androidx.fragment.app.*
import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.Fragments.AddObservationFragment
import com.noque.svampeatlas.View.Fragments.AddObservationFragments.DetailsFragment
import com.noque.svampeatlas.View.Fragments.AddObservationFragments.LocalityFragment
import com.noque.svampeatlas.View.Fragments.AddObservationFragments.SpeciesFragment


class InformationAdapter(val context: Context?, val categories: Array<AddObservationFragment.Category>, fm: FragmentManager, behavior: Int) : FragmentPagerAdapter(fm, behavior) {

    override fun getItem(position: Int): Fragment {
        when (categories[position]) {
            AddObservationFragment.Category.SPECIES -> return SpeciesFragment()
            AddObservationFragment.Category.DETAILS -> return DetailsFragment()
            AddObservationFragment.Category.LOCALITY -> return LocalityFragment()
        }
    }

    override fun getCount(): Int {
        return categories.count()
    }

    override fun getPageTitle(position: Int): CharSequence? {
        when (categories[position]) {
            AddObservationFragment.Category.DETAILS -> return context?.resources?.getText(R.string.addObservationCategory_details)
            AddObservationFragment.Category.LOCALITY -> return context?.resources?.getText(R.string.addObservationCategory_location)
            AddObservationFragment.Category.SPECIES -> return context?.resources?.getText(R.string.addObservationCategory_species)
        }
    }
}