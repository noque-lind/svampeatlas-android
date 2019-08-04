package com.noque.svampeatlas.Adapters.AddObservationAdapters

import android.view.ViewGroup
import androidx.fragment.app.*
import com.noque.svampeatlas.View.Fragments.AddObservationFragment
import com.noque.svampeatlas.View.Fragments.AddObservationFragments.DetailsFragment
import com.noque.svampeatlas.View.Fragments.AddObservationFragments.LocalityFragment
import com.noque.svampeatlas.View.Fragments.AddObservationFragments.SpeciesFragment


class InformationAdapter(val categories: Array<AddObservationFragment.Categories>, fm: FragmentManager, behavior: Int) : FragmentPagerAdapter(fm, behavior) {

    override fun getItem(position: Int): Fragment {
        when (categories[position]) {
            AddObservationFragment.Categories.SPECIES -> return SpeciesFragment()
            AddObservationFragment.Categories.DETAILS -> return DetailsFragment()
            AddObservationFragment.Categories.LOCALITY -> return LocalityFragment()
        }
    }

    override fun getCount(): Int {
        return categories.count()
    }
}