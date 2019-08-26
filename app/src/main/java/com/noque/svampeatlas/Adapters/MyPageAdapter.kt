package com.noque.svampeatlas.Adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Model.*
import com.noque.svampeatlas.R
import com.noque.svampeatlas.ViewHolders.*
import com.noque.svampeatlas.ViewModel.NewObservationViewModel
import retrofit2.http.Header

class MyPageAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        val TAG = "MyPageAdapter"
    }

    data class Item(val itemType: ItemType) {
        enum class ViewType {
            NOTIFICATION,
            RELOADER,
            OBSERVATION,
            ERROR,
            LOGOUT;

            companion object {
                val values = values()
            }
        }

        val viewType: ViewType get() {
            return when (itemType) {
                is ItemType.Notification -> ViewType.NOTIFICATION
                is ItemType.LogOut -> ViewType.LOGOUT
                is ItemType.Observation -> ViewType.OBSERVATION
                is ItemType.Reloader -> ViewType.RELOADER
                is ItemType.Error -> ViewType.ERROR
            }
        }
    }

    sealed class ItemType {
        enum class Category {
            NOTIFICATIONS,
            OBSERVATIONS
        }

        class Notification(val notification: com.noque.svampeatlas.Model.Notification): ItemType()
        class Observation(val observation: com.noque.svampeatlas.Model.Observation): ItemType()
        class Reloader(val category: Category, val offset: Int): ItemType()
        class LogOut: ItemType()
        class Error(val error: AppError): ItemType()
    }

        var observationSelected: ((Observation) -> Unit)? = null
        var loadAdditionalDataSelected: ((category: ItemType.Category, offset: Int) -> Unit)? = null
        var notificationSelected: ((Notification) -> Unit)? = null
        var logOutButtonPressed: (() -> Unit)? = null


        private var notifications = listOf<Section<Item>>()
        private var observations = listOf<Section<Item>>()


        private val sections = mutableListOf<Section<Item>>()

        private val onClickListener = View.OnClickListener { view ->
            (view.tag as? RecyclerView.ViewHolder)?.adapterPosition?.let {
                getItem(it)?.let {
                    when (it.itemType) {
                        is ItemType.Notification -> { notificationSelected?.invoke(it.itemType.notification) }
                        is ItemType.LogOut -> { logOutButtonPressed?.invoke() }
                        is ItemType.Observation -> { observationSelected?.invoke(it.itemType.observation) }
                        is ItemType.Reloader -> { loadAdditionalDataSelected?.invoke(it.itemType.category, it.itemType.offset) }
                        is ItemType.Error -> {}
                    }
                }
            }
        }

        fun configureNotifications(notifications: List<Section<Item>>) {
            this.notifications = notifications
            configure()
        }

        fun configureObservations(observations: List<Section<Item>>) {
            this.observations = observations
            configure()
        }

        private fun configure() {
            sections.clear()
            sections.addAll(notifications)
            sections.addAll(observations)
            sections.add(Section(null, listOf(Item(ItemType.LogOut()))))
            notifyDataSetChanged()
        }

        private fun getSection(position: Int): Section<Item>? {
            var currentPosition = 0

            sections.forEach {
                if (position == currentPosition) {
                    return it
                }
                currentPosition += it.count()
            }
            return null
        }

        private fun getItem(position: Int): Item? {
            var currentPosition = 0

            sections.forEach {
                if (position >= currentPosition && position <= (currentPosition + it.count() - 1)) {
                    return it.getItem(position - currentPosition)
                }
                currentPosition += it.count()
            }
            return null
        }

        override fun getItemViewType(position: Int): Int {
            var currentPosition = 0

            sections.forEach {
                if (position >= currentPosition && position <= (currentPosition + it.count() - 1)) {
                    val viewType = it.viewType(position - currentPosition)
                    return when (viewType) {
                        Section.ViewType.HEADER -> {  viewType.ordinal }
                        Section.ViewType.ITEM -> { (getItem(position)?.viewType?.ordinal)?.plus(1) ?: viewType.ordinal }
                    }
                }
                currentPosition += it.count()

            }
            return super.getItemViewType(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            var view: View
            var viewHolder: RecyclerView.ViewHolder

            if (viewType >= 1) {
                when (Item.ViewType.values[viewType - 1]) {
                    Item.ViewType.NOTIFICATION -> {
                        Log.d(TAG, "Creating a notification view holder")
                        view = layoutInflater.inflate(R.layout.item_notification, parent, false)
                        viewHolder = NotificationViewHolder(view)
                        viewHolder.itemView.tag = viewHolder
                        viewHolder.itemView.setOnClickListener(onClickListener)
                    }

                    Item.ViewType.OBSERVATION -> {
                        Log.d(TAG, "Creating an observation view holder")
                        view = layoutInflater.inflate(R.layout.item_observation, parent, false)
                        viewHolder = ObservationViewHolder(view)
                        viewHolder.itemView.tag = viewHolder
                        viewHolder.itemView.setOnClickListener(onClickListener)
                    }

                    Item.ViewType.LOGOUT -> {
                        view = layoutInflater.inflate(R.layout.item_log_out, parent, false)
                        viewHolder = LogOutViewHolder(view)
                        viewHolder.itemView.tag = viewHolder
                        viewHolder.itemView.setOnClickListener(onClickListener)

                    }

                    Item.ViewType.ERROR -> {
                        view = layoutInflater.inflate(R.layout.item_error, parent, false)
                        viewHolder = ErrorViewHolder(view)
                        viewHolder.itemView.tag = viewHolder
                        viewHolder.itemView.setOnClickListener(onClickListener)
                    }

                    Item.ViewType.RELOADER -> {
                        view = layoutInflater.inflate(R.layout.item_reloader, parent, false)
                        viewHolder = ReloaderViewHolder(view)
                        viewHolder.itemView.tag = viewHolder
                        viewHolder.itemView.setOnClickListener(onClickListener)
                    }
                }
            } else {
                view = layoutInflater.inflate(R.layout.item_header, parent, false)
                viewHolder = HeaderViewHolder(view)
            }
            return viewHolder
        }

        override fun getItemCount(): Int {
            var count = 0

            sections.forEach {
                count += it.count()
            }

            return count
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder.itemViewType) {
                0 -> (holder as? HeaderViewHolder)?.configure((getSection(position)?.title() ?: ""))
            }

            getItem(position)?.let {
                when (it.itemType) {
                    is ItemType.Notification -> { (holder as? NotificationViewHolder)?.configure(it.itemType.notification) }
                    is ItemType.LogOut -> {}
                    is ItemType.Observation -> { (holder as? ObservationViewHolder)?.configure(it.itemType.observation) }
                    is ItemType.Error -> { (holder as? ErrorViewHolder)?.configure(it.itemType.error) }
                    is ItemType.Reloader -> {}
                }
            }
        }

        override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
            notificationSelected = null
            super.onDetachedFromRecyclerView(recyclerView)
        }
    }
