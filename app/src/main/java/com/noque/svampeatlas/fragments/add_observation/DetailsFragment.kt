package com.noque.svampeatlas.fragments.add_observation

import android.app.DatePickerDialog
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.adapters.add_observation.DetailsAdapter
import com.noque.svampeatlas.R
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.view_models.NewObservationViewModel
import kotlinx.android.synthetic.main.fragment_add_observation_details.*
import java.util.*

class DetailsFragment : Fragment() {

    companion object {
        val TAG = "AddObs.DetailsFragment"
    }

    enum class Categories {
        DATE,
        Determinators,
        VEGETATIONTYPE,
        SUBSTRATE,
        HOST,
        NOTES,
        ECOLOGYNOTES;

        companion object {
            val values = values()
        }
    }

    // Views

    private var recyclerView by autoCleared<RecyclerView> {
        it?.adapter = null
    }

    // View Models
    private val newObservationViewModel: NewObservationViewModel by viewModels({ requireParentFragment() })

    // Adapters

    private val adapter: DetailsAdapter by lazy {
        val adapter = DetailsAdapter(resources, Categories.values)

        adapter.categoryClicked = {
            when (it) {
                Categories.DATE -> showDatePicker()
                Categories.SUBSTRATE -> showPicker(DetailsPickerFragment.Type.SUBSTRATEPICKER)
                Categories.VEGETATIONTYPE -> showPicker(DetailsPickerFragment.Type.VEGETATIONTYPEPICKER)
                Categories.HOST -> showPicker(DetailsPickerFragment.Type.HOSTPICKER)
                else -> {}
            }
        }

        adapter.onTextInputChanged = { category, text ->
            when (category) {
                Categories.NOTES -> {
                    newObservationViewModel.setNotes(text)
                }
                Categories.ECOLOGYNOTES -> {
                    newObservationViewModel.setEcologyNotes(text)
                }
                else -> {
                }
            }
        }

        adapter
    }

    // Listeners

    private val datePickerListener =
        DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, monthOfYear, dayOfMonth)
            newObservationViewModel.setObservationDate(cal.time)
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        return inflater.inflate(R.layout.fragment_add_observation_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = addObservationFragment_details_recyclerView
        setupView()
        setupViewModels()
    }

    private fun setupView() {
        recyclerView.apply {
            adapter = this@DetailsFragment.adapter
            layoutManager = LinearLayoutManager(context)

            val dividerItemDecoration =
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            dividerItemDecoration.setDrawable(ColorDrawable(ResourcesCompat.getColor(resources, R.color.colorWhite, null)))
            addItemDecoration(dividerItemDecoration)
        }
    }

    private fun setupViewModels() {
        newObservationViewModel.user.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            adapter.user = it
            adapter.updateCategory(Categories.Determinators)
        })

            newObservationViewModel.substrate.observe(
                viewLifecycleOwner,
                androidx.lifecycle.Observer {
                    adapter.substrate = it
                    adapter.updateCategory(Categories.SUBSTRATE)
                })

            newObservationViewModel.vegetationType.observe(
                viewLifecycleOwner,
                androidx.lifecycle.Observer {
                    adapter.vegetationType = it
                    adapter.updateCategory(Categories.VEGETATIONTYPE)
                })


            newObservationViewModel.hosts.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                adapter.hosts = it
                adapter.updateCategory(Categories.HOST)
            })


            newObservationViewModel.observationDate.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
                adapter.date = it
                adapter.updateCategory(Categories.DATE)
            })

        newObservationViewModel.notes.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it != adapter.notes) {
                adapter.notes = it
                adapter.updateCategory(Categories.NOTES)
            }
        })

        newObservationViewModel.ecologyNotes.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it != adapter.ecologyNotes) {
                adapter.ecologyNotes = it
                adapter.updateCategory(Categories.ECOLOGYNOTES)
            }
        })
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.DatePicker,
            datePickerListener,
            Calendar.getInstance().get(Calendar.YEAR),
            Calendar.getInstance().get(Calendar.MONTH),
            Calendar.getInstance().get(Calendar.DATE)
        )

        datePickerDialog.datePicker.maxDate = Calendar.getInstance().time.time
        datePickerDialog.show()
    }

    private fun showPicker(type: DetailsPickerFragment.Type) {
        val bundle = Bundle()
        bundle.putSerializable(DetailsPickerFragment.TYPE_KEY, type)

        val dialog = DetailsPickerFragment()
        dialog.arguments = bundle
        dialog.show(childFragmentManager, null)
    }
}