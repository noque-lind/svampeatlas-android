package com.noque.svampeatlas.fragments

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.NotebookAdapter
import com.noque.svampeatlas.extensions.dpToPx
import com.noque.svampeatlas.extensions.toSimpleString
import com.noque.svampeatlas.models.NewObservation
import com.noque.svampeatlas.models.Section
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.view_models.MushroomsViewModel
import com.noque.svampeatlas.view_models.NotesFragmentViewModel
import com.noque.svampeatlas.views.BlankActivity
import kotlinx.android.synthetic.main.action_view_add_notebook_entry.view.*
import kotlinx.android.synthetic.main.fragment_notebook.*
import java.io.File

class NotesFragment: Fragment() {

    companion object {
        const val RELOAD_DATA_KEY = "RELOAD_DATA_KEY"
    }


    // Views
    private var toolbar by autoCleared<Toolbar>()
    private var recyclerView by autoCleared<RecyclerView> {
        it?.adapter = null
    }

    private val notebookAdapter by lazy {
        NotebookAdapter().apply {
            listener = object: NotebookAdapter.Listener {
                override fun newObservationSelected(newObservation: NewObservation) {
                   val action = NotesFragmentDirections.actionNotesFragmentToAddObservationFragment()
                    action.type = AddObservationFragment.Type.EditNote
                    action.id = newObservation.creationDate.time
                    findNavController().navigate(action)
                }
            }
        }
    }
    private val viewModel by viewModels<NotesFragmentViewModel>()


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
        setupViewModel()
    }


    private fun setupViews() {
        (requireActivity() as BlankActivity).setSupportActionBar(toolbar)
        recyclerView.apply {
            val myHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
                override fun onChildDrawOver(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder?,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    super.onChildDrawOver(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }

                override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float {
                    return super.getMoveThreshold(viewHolder)
                }

                override fun onChildDraw(
                    c: Canvas,
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    dX: Float,
                    dY: Float,
                    actionState: Int,
                    isCurrentlyActive: Boolean
                ) {
                    val rightMargin = 32.dpToPx(requireContext())
                    val iconSize = 16.dpToPx(requireContext())
                    val icon = ResourcesCompat.getDrawable(resources, R.drawable.ic_delete_black_24dp, null)
                    val background = ColorDrawable(ResourcesCompat.getColor(resources, R.color.colorRed, null))

                    icon?.bounds = Rect(
                        viewHolder.itemView.right - iconSize * 2 - rightMargin,
                        viewHolder.itemView.top + (viewHolder.itemView.height / 2) - iconSize,
                        viewHolder.itemView.right - rightMargin,
                        viewHolder.itemView.bottom - (viewHolder.itemView.height / 2) + iconSize
                    )


                    background.bounds = Rect(
                        viewHolder.itemView.right + dX.toInt(),
                        viewHolder.itemView.top + resources.getDimension(R.dimen.item_mushroom_top_margin).toInt(),
                        viewHolder.itemView.right,
                        viewHolder.itemView.bottom - resources.getDimension(R.dimen.item_mushroom_bottom_margin).toInt()
                    )

                    background.draw(c)
                    icon?.draw(c)

                    super.onChildDraw(
                        c,
                        recyclerView,
                        viewHolder,
                        dX,
                        dY,
                        actionState,
                        isCurrentlyActive
                    )
                }


                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    notebookAdapter.sections.getItem(viewHolder.adapterPosition).let {
                        viewModel.deleteNote((it as NotebookAdapter.Items.Note).newObservation, viewHolder.adapterPosition)
                    }
                }
            })
            myHelper.attachToRecyclerView(this)
            adapter = notebookAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(
            RELOAD_DATA_KEY)?.observe(viewLifecycleOwner, Observer {
            if (it) {
                viewModel.getNotes()
            }
            findNavController().currentBackStackEntry?.savedStateHandle?.remove<Boolean>(RELOAD_DATA_KEY)
        })
    }


    private fun setupViewModel() {
        viewModel.notes.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Items -> {
                    val dateSortedNotes = mutableMapOf<String, MutableList<NewObservation>>()
                    it.items.forEach {
                        if (dateSortedNotes.containsKey(it.creationDate.toSimpleString())) {
                            dateSortedNotes[it.creationDate.toSimpleString()]?.add(it)
                        } else {
                            dateSortedNotes[it.creationDate.toSimpleString()] = mutableListOf(it)
                        }
                    }
                    notebookAdapter.setSections(dateSortedNotes.map { Section(it.key, State.Items<List<NotebookAdapter.Items>>(it.value.map { NotebookAdapter.Items.Note(it) }))})
                }
                is State.Empty -> notebookAdapter.setSections(listOf(Section(null, State.Empty())))
                is State.Loading -> notebookAdapter.setSections(listOf(Section(null, State.Loading())))
                is State.Error -> notebookAdapter.setSections(listOf(Section(null, State.Error(it.error))))
            }

        })

        viewModel.noteDeleted.observe(viewLifecycleOwner, Observer {
            notebookAdapter.sections.deleteItem(it)
            notebookAdapter.notifyItemRemoved(it)
        })
    }

    private fun setup() {
    }





}