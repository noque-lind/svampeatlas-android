package com.noque.svampeatlas.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.NotebookAdapter
import com.noque.svampeatlas.utilities.autoCleared
import kotlinx.android.synthetic.main.fragment_notebook.*

class NotesFragment: Fragment() {


    // Views
    private var toolbar by autoCleared<Toolbar>()
    private var recyclerView by autoCleared<RecyclerView> {
        it?.adapter = null
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notebook, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = notebookFragment_toolbar
        recyclerView = notebookFragment_recyclerView
        setupViews()
    }


    private fun setupViews() {
        recyclerView.apply {
            adapter = NotebookAdapter()
        }


    }





}