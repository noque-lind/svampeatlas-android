package com.noque.svampeatlas.fragments


import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.adapters.MushroomListAdapter

import com.noque.svampeatlas.R
import com.noque.svampeatlas.view_models.MushroomsViewModel
import com.noque.svampeatlas.models.State
import kotlinx.android.synthetic.main.fragment_mushroom.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.tabs.TabLayout
import com.noque.svampeatlas.extensions.changeColor
import com.noque.svampeatlas.extensions.dpToPx
import com.noque.svampeatlas.extensions.italized
import com.noque.svampeatlas.extensions.pxToDp
import com.noque.svampeatlas.fragments.modals.DownloaderFragment
import com.noque.svampeatlas.views.BackgroundView
import com.noque.svampeatlas.views.BlankActivity
import com.noque.svampeatlas.views.SearchBarListener
import com.noque.svampeatlas.views.SearchBarView
import com.noque.svampeatlas.view_models.factories.MushroomsViewModelFactory
import kotlinx.android.synthetic.main.custom_toast.*
import kotlinx.android.synthetic.main.custom_toast.view.*


class MushroomFragment : Fragment() {

    companion object {
        val TAG = "MushroomFragment"
    }


    // Views

    private var recyclerView: RecyclerView? = null
    private var backgroundView: BackgroundView? = null
    private var searchBarView: SearchBarView? = null
    private var swipeRefreshLayout: SwipeRefreshLayout? = null
    private var tabLayout: TabLayout? = null

    // View models

    private val mushroomsViewModel by lazy {
        ViewModelProvider(this, MushroomsViewModelFactory(MushroomsViewModel.Category.SPECIES, requireActivity().application))[MushroomsViewModel::class.java]
    }


    // Adapters

    private val mushroomListAdapter: MushroomListAdapter by lazy {
        val adapter = MushroomListAdapter()

        adapter.setOnClickListener { mushroom ->
            val action = MushroomFragmentDirections.actionGlobalMushroomDetailsFragment(
                mushroom.id,
                DetailsFragment.TakesSelection.NO,
                DetailsFragment.Context.SPECIES,
                null,
                null
            )
            findNavController().navigate(action)
        }

        adapter
    }

    // Listeners

    private val searchBarListener by lazy {
        object : SearchBarListener {
            override fun newSearch(entry: String) {
                mushroomsViewModel.search(entry, true)
            }

            override fun clearedSearchEntry() {
                mushroomsViewModel.reloadData()
            }
        }
    }

    private val onScrollListener by lazy {
        object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (!recyclerView.canScrollVertically(-1)) {
                    searchBarView?.expand()
                } else if (dy > 0) {
                    searchBarView?.collapse()
                }
            }
        }
    }

    private val onRefreshListener  by lazy {
        SwipeRefreshLayout.OnRefreshListener {
            mushroomsViewModel.reloadData()
            swipeRefreshLayout?.isRefreshing = false
        }
    }

    private val onTapSelectedListener by lazy {
        object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {
                when (MushroomsViewModel.Category.values[tab.position]) {
                    MushroomsViewModel.Category.FAVORITES -> {
                        searchBarView?.visibility = View.GONE
                        recyclerView?.setPadding(0, 0, 0, 0)

                    }
                    MushroomsViewModel.Category.SPECIES -> {
                        searchBarView?.visibility = View.VISIBLE
                        recyclerView?.setPadding(
                            0,
                            (resources.getDimension(R.dimen.searchbar_view_height) + resources.getDimension(
                                R.dimen.searchbar_top_margin
                            )).toInt(),
                            0,
                            0
                        )
                    }
                }
            }

            override fun onTabUnselected(p0: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab) {
                val category = MushroomsViewModel.Category.values[tab.position]
                mushroomsViewModel.selectCategory(category)

                when (category) {
                    MushroomsViewModel.Category.FAVORITES -> {
                        searchBarView?.visibility = View.GONE
                        recyclerView?.setPadding(0, 0, 0, 0)

                    }
                    MushroomsViewModel.Category.SPECIES -> {
                        searchBarView?.visibility = View.VISIBLE
                        recyclerView?.setPadding(
                            0,
                            (resources.getDimension(R.dimen.searchbar_view_height) + resources.getDimension(
                                R.dimen.searchbar_top_margin
                            )).toInt(),
                            0,
                            0
                        )
                    }
                }
            }
        }
    }

    private val imageSwipedCallback by lazy {
        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
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
                val iconSize = 30.dpToPx(requireContext())
                val icon: Drawable
                val background: ColorDrawable

                if (mushroomsViewModel.selectedCategory.value == MushroomsViewModel.Category.FAVORITES) {
                    background =
                        ColorDrawable(ResourcesCompat.getColor(resources, R.color.colorRed, null))
                    icon = resources.getDrawable(R.drawable.icon_favorite_remove, null)
                } else {
                    background =
                        ColorDrawable(ResourcesCompat.getColor(resources, R.color.colorGreen, null))
                    icon = resources.getDrawable(R.drawable.icon_favorite_make, null)
                }


                icon.bounds = Rect(
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
                icon.draw(c)

                swipeRefreshLayout?.isEnabled = !isCurrentlyActive

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
                if (mushroomsViewModel.selectedCategory.value == MushroomsViewModel.Category.FAVORITES) {
                    mushroomsViewModel.unFavoriteMushroomAt(viewHolder.adapterPosition)
                } else {
                    mushroomsViewModel.favoriteMushroomAt(viewHolder.adapterPosition)
                    mushroomListAdapter.notifyItemChanged(viewHolder.adapterPosition)
                }

            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mushroom, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupView()
        setupViewModels()
    }


    private fun initViews() {
        recyclerView = mushroomFragment_recyclerView
        backgroundView = mushroomFragment_backgroundView
        searchBarView = mushroomFragment_searchBarView
        swipeRefreshLayout = mushroomFragment_swipeRefreshLayout
        tabLayout = mushroomFragment_tabLayout
    }

    private fun setupView() {
        (requireActivity() as BlankActivity).setSupportActionBar(mushroomFragment_toolbar)

        tabLayout?.apply {
            MushroomsViewModel.Category.values.forEach {
                val tab = this.newTab()

                when (it) {
                    MushroomsViewModel.Category.FAVORITES -> {
                        tab.text = resources.getText(R.string.mushroomVC_category_favorites)
                    }
                    MushroomsViewModel.Category.SPECIES -> {
                        tab.text = resources.getText(R.string.mushroomVC_category_species)
                    }
                }

                tab.tag = it
                this.addTab(tab)

            }

            this.addOnTabSelectedListener(onTapSelectedListener)
        }


        recyclerView?.apply {
            val myHelper = ItemTouchHelper(imageSwipedCallback)
            myHelper.attachToRecyclerView(this)


            val layoutManager = LinearLayoutManager(context)
            layoutManager.orientation = RecyclerView.VERTICAL
            this.layoutManager = layoutManager
            this.addOnScrollListener(onScrollListener)
            this.adapter = mushroomListAdapter
            runLayoutAnimation()
        }

        swipeRefreshLayout?.apply {
            setOnRefreshListener(onRefreshListener)
        }

        searchBarView?.apply {
            setListener(searchBarListener)
        }
    }

    private fun setupViewModels() {
        mushroomsViewModel.selectedCategory.observe(viewLifecycleOwner, Observer {
            tabLayout?.getTabAt(it.ordinal)?.select()
        })

        mushroomsViewModel.mushroomsState.observe(viewLifecycleOwner, Observer {
            backgroundView?.reset()

            when (it) {
                is State.Loading -> {
                    mushroomListAdapter.updateData(listOf())
                    backgroundView?.setLoading()
                }
                is State.Items -> {
                    runLayoutAnimation()
                    mushroomListAdapter.updateData(it.items)

                }

                is State.Error -> {
                    backgroundView?.setError(it.error)
                }
                else -> {}
            }
        })

        mushroomsViewModel.favoringState.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Error -> {
                    mushroomsViewModel.resetFavoritizingState()
                    val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_elmessageview_failure).changeColor(ResourcesCompat.getColor(resources, R.color.colorRed, null))
                    createToast(it.error.title, it.error.message, bitmap)
                }

                is State.Items -> {
                    mushroomsViewModel.resetFavoritizingState()
                    val bitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_elmessageview_failure).changeColor(ResourcesCompat.getColor(resources, R.color.colorGreen, null))
                    createToast(getString(R.string.mushroomVC_favoriteSucces_title, it.items.localizedName ?: it.items.fullName.italized()), getString(R.string.mushroomVC_favoriteSucces_message), bitmap)
                }
                else -> {}
            }
        })
    }

    private fun runLayoutAnimation() {
        val controller = AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.item_animation)

        recyclerView?.layoutAnimation = controller
        recyclerView?.scheduleLayoutAnimation()
    }

    private fun createToast(title: String, message: String, bitmap: Bitmap) {

        val container = custom_toast_container
        val layout = layoutInflater.inflate(R.layout.custom_toast, container)

        layout.customToast_titleTextView.text = title
        layout.customToast_messageTextView.text = message
        layout.customToast_imageView.setImageBitmap(bitmap)

        with(Toast(context)) {

            setGravity(Gravity.BOTTOM, 0, 16.pxToDp(context))
            duration = Toast.LENGTH_LONG
            view = layout
            show()
        }
    }

override fun onDestroyView() {
    recyclerView?.adapter = null
    recyclerView = null
    backgroundView = null
    searchBarView = null
    swipeRefreshLayout = null
    swipeRefreshLayout = null
    tabLayout = null
    super.onDestroyView()
}
}