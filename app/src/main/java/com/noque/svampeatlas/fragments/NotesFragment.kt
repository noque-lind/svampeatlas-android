package com.noque.svampeatlas.fragments

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.NotebookAdapter
import com.noque.svampeatlas.extensions.*
import com.noque.svampeatlas.fragments.modals.DownloaderFragment
import com.noque.svampeatlas.models.NewObservation
import com.noque.svampeatlas.models.Section
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.view_models.NotesFragmentViewModel
import com.noque.svampeatlas.views.BackgroundView
import com.noque.svampeatlas.views.BlankActivity
import kotlinx.android.synthetic.main.action_view_add_notebook_button.view.*
import kotlinx.android.synthetic.main.fragment_notebook.*

class NotesFragment: Fragment() {

    companion object {
        const val RELOAD_DATA_KEY = "RELOAD_DATA_KEY"
    }

    // Views
    private var toolbar by autoCleared<Toolbar>()
    private var recyclerView by autoCleared<RecyclerView> {
        it?.adapter = null
    }
    private var backgroundView by autoCleared<BackgroundView>()

    private val notebookAdapter by lazy {
        NotebookAdapter().apply {
            listener = object: NotebookAdapter.Listener {
                override fun newObservationSelected(newObservation: NewObservation) {
                   val action = NotesFragmentDirections.actionNotesFragmentToAddObservationFragment()
                    action.context = AddObservationFragment.Context.EditNote
                    action.id = newObservation.creationDate.time
                    findNavController().navigate(action)
                }

                override fun downloadForOfflinePressed() {
                    DownloaderFragment().show(parentFragmentManager, null)
                    this@apply.sections.deleteItem(0)
                    this@apply.notifyItemRemoved(0)
                }

                override fun uploadNewObservation(newObservation: NewObservation) {
                    val action = NotesFragmentDirections.actionNotesFragmentToAddObservationFragment()
                    action.context = AddObservationFragment.Context.UploadNote
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
                    val action = NotesFragmentDirections.actionNotesFragmentToAddObservationFragment().setContext(AddObservationFragment.Context.Note)
                    findNavController().navigate(action)
                }
            }
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_notebookFragment_redownloadOffline) {
            DownloaderFragment().show(parentFragmentManager, null)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar = notebookFragment_toolbar
        recyclerView = notebookFragment_recyclerView
        backgroundView = notebookFragment_backgroundView
        setupViews()
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
                        if (dateSortedNotes.containsKey(it.creationDate.toDatabaseName())) {
                            dateSortedNotes[it.creationDate.toDatabaseName()]?.add(it)
                        } else {
                            dateSortedNotes[it.creationDate.toDatabaseName()] = mutableListOf(it)
                        }
                    }
                    notebookAdapter.setSections(dateSortedNotes.map { Section(it.key.toDate().toReadableDate(true, true), State.Items<List<NotebookAdapter.Items>>(it.value.map { NotebookAdapter.Items.Note(it) }))})
                }
                is State.Empty -> notebookAdapter.setSections(listOf(Section(null, State.Empty())))
                is State.Loading -> notebookAdapter.setSections(listOf(Section(null, State.Loading())))
                is State.Error -> notebookAdapter.setSections(listOf(Section(null, State.Error(it.error))))
            }

        })
    }
}