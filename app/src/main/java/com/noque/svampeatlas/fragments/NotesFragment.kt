package com.noque.svampeatlas.fragments

import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.annotation.DimenRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.NotebookAdapter
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.views.BlankActivity
import kotlinx.android.synthetic.main.action_view_add_notebook_entry.view.*
import kotlinx.android.synthetic.main.fragment_notebook.*

class NotesFragment: Fragment() {


    // Views
    private var toolbar by autoCleared<Toolbar>()
    private var recyclerView by autoCleared<RecyclerView> {
        it?.adapter = null
    }

    private val notebookAdapter by lazy { NotebookAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_notebook, container, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.notebook_fragment_menu, menu)

        menu.findItem(R.id.menu_notebookFragment_addEntry)?.let {
            (it.actionView as? LinearLayout)?.apply {
                actionView_addNotebookEntry.setOnClickListener {
                    val action = NotesFragmentDirections.actionNotesFragmentToAddObservationFragment().setType(AddObservationFragment.Type.Note)
                    findNavController().navigate(action)
                }
            }
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = notebookFragment_toolbar
        recyclerView = notebookFragment_recyclerView
        setupViews()
        setup()
    }


    private fun setupViews() {
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
        recyclerView.apply {
            adapter = notebookAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

    }

    private fun setup() {
    }





}