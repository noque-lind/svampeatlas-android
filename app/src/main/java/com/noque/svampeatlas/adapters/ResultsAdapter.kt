package com.noque.svampeatlas.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.PredictionResult
import com.noque.svampeatlas.view_holders.ErrorViewHolder
import com.noque.svampeatlas.view_holders.ReloaderViewHolder
import com.noque.svampeatlas.view_holders.ResultItemViewHolder

class ResultsAdapter: RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface Listener {
        fun reloadSelected()
        fun predictionResultSelected(predictionResult: PredictionResult)
    }

    enum class ViewType {
        RESULT,
        ERROR,
        RETRY;

        companion object {
            val values = values()
        }
    }


    private var results = listOf<PredictionResult>()
    private var error: AppError? = null

    private var listener: Listener? = null

    fun configure(results: List<PredictionResult>) {
        this.results = results
        notifyDataSetChanged()
    }

    fun configure(error: AppError) {
        this.results = listOf()
        this.error = error
        notifyDataSetChanged()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    private val onItemClickListener = View.OnClickListener {
        when (val viewHolder = it.tag) {
            is ReloaderViewHolder -> { listener?.reloadSelected() }
            is ResultItemViewHolder -> { results.getOrNull(viewHolder.adapterPosition)?.let { listener?.predictionResultSelected(it) } }
        }
    }


    override fun getItemCount(): Int {
        return if (error != null) 2 else results.count() + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (error != null) {
            if (position == 0) ViewType.ERROR.ordinal else ViewType.RETRY.ordinal
        } else {
            if (position > results.lastIndex) ViewType.RETRY.ordinal else ViewType.RESULT.ordinal
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view: View
        val viewHolder: RecyclerView.ViewHolder

        when (ViewType.values[viewType]) {
            ViewType.RESULT -> {
                view = layoutInflater.inflate(R.layout.item_result, parent, false)
                viewHolder = ResultItemViewHolder(view)
            }
            ViewType.RETRY -> {
                view = layoutInflater.inflate(R.layout.item_reloader, parent, false)
                viewHolder = ReloaderViewHolder(view)
            }

            ViewType.ERROR -> {
                view = layoutInflater.inflate(R.layout.item_error, parent, false)
                viewHolder = ErrorViewHolder(view)
            }
        }


        view.tag = viewHolder
        view.setOnClickListener(onItemClickListener)
        return viewHolder
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ResultItemViewHolder -> { results.getOrNull(position)?.let { holder.configure(it.mushroom, it.score) } }
            is ErrorViewHolder -> { error?.let { holder.configure(it) } }
            is ReloaderViewHolder -> { holder.configure(ReloaderViewHolder.Type.RELOAD) }
        }
    }

}