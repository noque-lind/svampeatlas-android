package com.noque.svampeatlas.View.Fragments.AddObservationFragments

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.noque.svampeatlas.Adapters.AddObservationAdapters.DetailsAdapter
import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.SubstratePickerFragment
import kotlinx.android.synthetic.main.fragment_add_observation_details.*
import org.intellij.lang.annotations.Subst
import java.sql.Date
import java.time.Instant
import java.util.*

class DetailsFragment : Fragment() {

    enum class Categories {
        DATE,
        VEGETATIONTYPE,
        SUBSTRATE,
        HOST
    }

    private val categories = Categories.values()
    private lateinit var adapter: DetailsAdapter

    private val detailsAdapterListener = object: DetailsAdapter.Listener {
        override fun onViewClicked(view: View) {
            val position = addObservationFragment_details_recyclerView.getChildAdapterPosition(view)
            when (categories[position]) {
               Categories.DATE -> yes()
                Categories.SUBSTRATE -> showSubstratePicker()
                Categories.VEGETATIONTYPE -> showVegetationTypePicker()
           }
        }

    }

    private val datePickerListener = object: DatePickerDialog.OnDateSetListener {
        override fun onDateSet(p0: DatePicker?, year: Int, monthOfYear: Int, dayOfMonth: Int) {

        }

    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_observation_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
    }

    fun setupView() {
        adapter = DetailsAdapter(categories)
        adapter.setListener(detailsAdapterListener)

        addObservationFragment_details_recyclerView.adapter = adapter
        addObservationFragment_details_recyclerView.layoutManager = LinearLayoutManager(context)


        val dividerItemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        dividerItemDecoration.setDrawable(ColorDrawable(resources.getColor(R.color.colorWhite)))

        addObservationFragment_details_recyclerView.addItemDecoration(dividerItemDecoration)
        adapter.notifyDataSetChanged()
    }

    fun yes() {
        val datePickerDialog = DatePickerDialog(context!!,
            datePickerListener,
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DATE)
            )

        datePickerDialog.datePicker.maxDate = Calendar.getInstance().timeInMillis
        datePickerDialog.show()
    }

    fun showSubstratePicker() {
        val dialog = SubstratePickerFragment(SubstratePickerFragment.Type.SUBSTRATEPICKER)
        dialog.show(childFragmentManager, null)
    }

    fun showVegetationTypePicker() {
        val dialog = SubstratePickerFragment(SubstratePickerFragment.Type.VEGETATIONTYPEPICKER)
        dialog.show(childFragmentManager, null)
    }

}