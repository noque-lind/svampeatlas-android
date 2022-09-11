package com.noque.svampeatlas.fragments.modals

import android.app.DownloadManager
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.content.*
import android.database.Cursor
import android.os.Bundle
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.State
import com.noque.svampeatlas.services.DataService
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.view_models.DownloaderViewModel
import com.noque.svampeatlas.views.BackgroundView
import kotlinx.android.synthetic.main.fragment_modal_download.*
import kotlinx.android.synthetic.main.item_loader.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*


class DownloaderFragment: DialogFragment() {


    // Views
    private var titleTextView by autoCleared<TextView>()
    private var messageTextView by autoCleared<TextView>()
    private var backgroundView by autoCleared<BackgroundView>()

    private val viewModel by viewModels<DownloaderViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_modal_download, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleTextView = downloaderFragment_titleTextView
        messageTextView = downloaderFragment_messageTextView
        backgroundView = downloaderFragment_errorView
        setupViews()
        setupViewModels()
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun setupViews() {
        titleTextView.text = getString(R.string.downloader_message_data)
    }

    private fun setupViewModels() {

        viewModel.state.observe(viewLifecycleOwner, Observer {
            isCancelable = true
            when (it) {
                is State.Items -> {
                    SharedPreferences.lastDownloadOfTaxon = Date()
                    dismiss()
                }
                is State.Empty -> {}
                is State.Loading -> {
                    isCancelable = false
                    backgroundView.setLoading()
                }
                is State.Error -> {
                    messageTextView.text = null
                    backgroundView.setErrorWithHandler(it.error, it.error.recoveryAction) {
                        viewModel.startDownload()
                    }
                }
            }
        })

        viewModel.loadingState.observe(viewLifecycleOwner, Observer {
            messageTextView.text = getString(it.resID)
        })
    }
}