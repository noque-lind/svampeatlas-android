package com.noque.svampeatlas.View

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Adapters.AddObservationAdapters.Section
import com.noque.svampeatlas.Adapters.AddObservationAdapters.SubstrateAdapater
import com.noque.svampeatlas.Adapters.AddObservationAdapters.SubstratesAdapter
import com.noque.svampeatlas.Model.State
import com.noque.svampeatlas.Model.Substrate
import com.noque.svampeatlas.Model.SubstrateGroup
import com.noque.svampeatlas.Model.VegetationType
import com.noque.svampeatlas.R
import com.noque.svampeatlas.ViewModel.SubstrateViewModel
import com.noque.svampeatlas.ViewModel.VegetationTypesViewModel
import kotlinx.android.synthetic.main.fragment_substrate_picker.*


class SubstratePickerFragment(private val type: Type): DialogFragment() {

    enum class Type {
        SUBSTRATEPICKER,
        VEGETATIONTYPEPICKER,
        HOSTPICKER
    }


    private val adapter = SubstrateAdapater()

//    private val adapter = SubstratesAdapter()
    lateinit var substrateViewModel: SubstrateViewModel
    lateinit var vegetationTypesViewModel: VegetationTypesViewModel


    private val substrateStateObserver = object: Observer<State<List<SubstrateGroup>>> {
        override fun onChanged(state: State<List<SubstrateGroup>>?) {
            when (state) {
                is State.Error -> substrateFragment_backgroundView.setError(state.error)
                is State.Items -> {
                    val sections = state.items.map {
                        Section<Substrate>(it.dkName, it.substrates)
                    }


//                    val sections = state.items.map {
//                        SubstratesAdapter.Section.Substrates(it.dkName, it.substrates)
//                    }
                    adapter.configure(sections)}
            }
        }
    }

    private val vegetationTypeStateObserver = object: Observer<State<List<VegetationType>>> {
        override fun onChanged(state: State<List<VegetationType>>?) {
            when (state) {
                is State.Error -> substrateFragment_backgroundView.setError(state.error)
                is State.Items -> {
                    Log.d("SUb", state.items.toString())
//                   adapter.configure(listOf(Section<VegetationType>))}
//           }
                }

            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_substrate_picker, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (type) {
            Type.VEGETATIONTYPEPICKER -> {
                substrateFragment_headerTextView.text = "Vælg vegetationstype"

                vegetationTypesViewModel = ViewModelProviders.of(this).get(VegetationTypesViewModel::class.java)
                vegetationTypesViewModel.state.observe(viewLifecycleOwner, vegetationTypeStateObserver)
                vegetationTypesViewModel.getVegetationTypes()
            }

            Type.SUBSTRATEPICKER -> {
                substrateFragment_headerTextView.text = "Vælg substrat"
                substrateViewModel = ViewModelProviders.of(this).get(SubstrateViewModel::class.java)
                substrateViewModel.state.observe(viewLifecycleOwner, substrateStateObserver)
                substrateViewModel.getSubstrateGroups()
            }
        }

        substrateFragment_recyclerView.adapter = adapter
        substrateFragment_recyclerView.layoutManager = LinearLayoutManager(context)
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.70).toInt()
        dialog?.window?.setLayout(width, height)
    }

}