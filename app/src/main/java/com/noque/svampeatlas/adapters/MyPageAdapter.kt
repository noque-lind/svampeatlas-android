package com.noque.svampeatlas.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.*
import com.noque.svampeatlas.R
import com.noque.svampeatlas.view_holders.*

class MyPageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    companion object {
        val TAG = "MyPageAdapter"
    }

    interface Listener {
        fun observationSelected(observation: Observation)
        fun getAdditionalData(category: Item.Category, atOffset: Int)
        fun notificationSelected(notification: Notification)
        fun logoutButtonSelected()
    }



    sealed class Item(viewType: ViewType) : com.noque.svampeatlas.models.Item<Item.ViewType>(viewType) {

        enum class ViewType : com.noque.svampeatlas.models.ViewType {
            NOTIFICATION,
            OBSERVATION,
            LOGOUT,
            LOADMORE;

            companion object {
                val values = values()
            }
        }

        enum class Category {
            NOTIFICATIONS,
            OBSERVATIONS
        }

        class Notification(val notification: com.noque.svampeatlas.models.Notification) :
            Item(ViewType.NOTIFICATION)

        class Observation(val observation: com.noque.svampeatlas.models.Observation) :
            Item(ViewType.OBSERVATION)

        class LoadMore(val category: Category, val offset: Int) : Item(ViewType.LOADMORE)
        class Logout : Item(ViewType.LOGOUT)
    }

    private val sections = Sections<Item.ViewType, Item>()

    private var listener: Listener? = null

    private var notifications = Section<Item>("Notifikationer")
    private var observations = Section<Item>("Observationer")


    private val onClickListener = View.OnClickListener { view ->
        when (val viewHolder = view.tag) {
            is LogOutViewHolder -> {
                listener?.logoutButtonSelected()
            }
            is ReloaderViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is Item.LoadMore -> {
                        listener?.getAdditionalData(item.category, item.offset)
                    }
                }
            }
            is NotificationViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is Item.Notification -> {
                        listener?.notificationSelected(item.notification)
                    }
                }
            }

            is ObservationViewHolder -> {
                when (val item = sections.getItem(viewHolder.adapterPosition)) {
                    is Item.Observation -> {
                        listener?.observationSelected(item.observation)
                    }
                }
            }
        }
    }

    init {
        sections.addSection(notifications)
        sections.addSection(observations)
        sections.addSection(Section.Builder<Item>().items(listOf(Item.Logout())).build())
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun configureNotificationsState(state: State<List<Item>>, title: String? = null) {
        if (title != null) this.notifications.setTitle(title)
        this.notifications.setState(state)
        notifyDataSetChanged()
    }

    fun configureObservationsState(state: State<List<Item>>, title: String? = null) {
        if (title != null) this.notifications.setTitle(title)
        this.observations.setState(state)
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int {
       return sections.getCount()
    }

    override fun getItemViewType(position: Int): Int {
        return sections.getViewTypeOrdinal(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view: View
        val viewHolder: RecyclerView.ViewHolder
        when (sections.getSectionViewType(viewType)) {
            Section.ViewType.HEADER -> {
                view = layoutInflater.inflate(R.layout.item_header, parent, false)
                viewHolder = HeaderViewHolder(view)
            }
            Section.ViewType.ERROR -> {
                view = layoutInflater.inflate(R.layout.item_error, parent, false)
                viewHolder = ErrorViewHolder(view)
            }
            Section.ViewType.LOADER -> {
                view = layoutInflater.inflate(R.layout.item_reloader, parent, false)
                viewHolder = ReloaderViewHolder(view)
            }
            Section.ViewType.ITEM -> {
                when (Item.ViewType.values[viewType - Section.ViewType.values.count()]) {
                    Item.ViewType.NOTIFICATION -> {
                        view = layoutInflater.inflate(R.layout.item_notification, parent, false)
                        viewHolder = NotificationViewHolder(view)
                    }
                    Item.ViewType.OBSERVATION -> {
                        view = layoutInflater.inflate(R.layout.item_observation, parent, false)
                        viewHolder = ObservationViewHolder(view)
                    }
                    Item.ViewType.LOGOUT -> {
                        view = layoutInflater.inflate(R.layout.item_log_out, parent, false)
                        viewHolder = LogOutViewHolder(view)
                    }
                    Item.ViewType.LOADMORE -> {
                        view = layoutInflater.inflate(R.layout.item_reloader, parent, false)
                        viewHolder = ReloaderViewHolder(view)
                    }
                }

                view.setOnClickListener(onClickListener)
                view.tag = viewHolder
            }
        }

        return viewHolder
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> { sections.getTitle(position)?.let { holder.configure(it) } }
            is ObservationViewHolder -> { when (val item = sections.getItem(position)) {
                is Item.Observation -> { holder.configure(item.observation) }
            } }

            is NotificationViewHolder -> {when (val item = sections.getItem(position)) {
                is Item.Notification -> { holder.configure(item.notification) }
            }}

            is ErrorViewHolder -> { sections.getError(position)?.let { holder.configure(it) } }
        }
    }
}
