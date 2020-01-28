package com.noque.svampeatlas.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.adapters.ResultsAdapter
import com.noque.svampeatlas.extensions.downloadImage
import com.noque.svampeatlas.extensions.italized
import com.noque.svampeatlas.extensions.red
import com.noque.svampeatlas.models.AppError
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.models.PredictionResult
import com.noque.svampeatlas.services.DataService
import kotlinx.android.synthetic.main.view_result.view.*
import kotlinx.android.synthetic.main.view_results.view.*
import org.w3c.dom.Text

class ResultsView(context: Context?, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var titleTextView: TextView
    private var messageTextView: TextView
    private var recyclerView: RecyclerView

    private val resultsAdapter by lazy { ResultsAdapter() }


    init {
        val inflater = LayoutInflater.from(getContext())
        inflater.inflate(R.layout.view_results, this)

        titleTextView = resultsView_titleTextView
        messageTextView = resultsView_messageTextView
        recyclerView = resultsView_recyclerView

        setupViews()
    }

    private fun setupViews() {
        recyclerView.apply {
            this.adapter = resultsAdapter
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        }
    }

    fun setListener(listener: ResultsAdapter.Listener) {
        resultsAdapter.setListener(listener)
    }

    fun showResults(results: List<PredictionResult>) {
        if (results.count() > 0 ) {
            titleTextView.text = resources.getString(R.string.resultsView_results_title, results.count().toString())
            messageTextView.setText(context.getString(R.string.resultsView_results_message).red())
        } else {
            titleTextView.setText(R.string.resultsView_noResults_title)
            messageTextView.setText(R.string.resultsView_noResults_message)
        }

        resultsAdapter.configure(results)
        recyclerView.scrollTo(0,0)
        recyclerView.layoutManager?.scrollToPosition(0)
    }

    fun showError(error: AppError) {
        resultsAdapter.configure(error)
    }
}