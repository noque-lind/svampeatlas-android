package com.noque.svampeatlas.fragments.modals

import android.app.DownloadManager
import android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.noque.svampeatlas.utilities.autoCleared
import com.noque.svampeatlas.view_models.DownloaderViewModel
import com.noque.svampeatlas.views.BackgroundView
import kotlinx.android.synthetic.main.fragment_modal_download.*
import kotlinx.android.synthetic.main.item_loader.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class DownloaderFragment: DialogFragment() {


    // Views
    private var titleTextView by autoCleared<TextView>()
    private var messageTextView by autoCleared<TextView>()
    private var spinner by autoCleared<ProgressBar>()
    private var backgroundView by autoCleared<BackgroundView>()


    private var downloadID: Long? = null

    // Listeners

//    private val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent) {
//            //Fetching the download id received with the broadcast
//            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
//            val some = intent.getLongExtra(DownloadManager.EXTR, -1)
//            //Checking if the received broadcast is for our enqueued download by matching download id
//            if (downloadID == id) {
//                Toast.makeText(requireContext(), "Download Completed", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    private val viewModel by viewModels<DownloaderViewModel>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        return inflater.inflate(R.layout.fragment_modal_download, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        context?.registerReceiver(onDownloadComplete, IntentFilter(ACTION_DOWNLOAD_COMPLETE))
////        type = arguments?.getSerializable(KEY_TYPE) as Type
        spinner = downloaderFragment_progressBar
        titleTextView = downloaderFragment_titleTextView
        messageTextView = downloaderFragment_messageTextView
        backgroundView = downloaderFragment_errorView
        initViews()
        setupViewModels()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.70).toInt()
        dialog?.window?.setLayout(width, height)
    }

    private fun initViews() {



        titleTextView.text = "Henter alle taxon"

    }

    private fun setupViewModels() {
        viewModel.state.observe(viewLifecycleOwner, Observer {
            when (it) {
                is State.Items -> {
                    dismiss()
                }
                is State.Empty -> {}
                is State.Loading -> {
                    backgroundView.visibility = View.GONE
                    spinner.visibility = View.VISIBLE}
                is State.Error -> {
                    backgroundView.visibility = View.VISIBLE
                    backgroundView.setErrorWithHandler(it.error, it.error.recoveryAction) {

                    }
                    spinner.visibility = View.GONE
                }
            }
        })

        viewModel.loadingState.observe(viewLifecycleOwner, Observer {
            messageTextView.text = it.message
        })
    }




//    fun download() {
//        downloadID = DataService.getInstance(requireContext()).testDownload()
//    }
//
//    suspend fun checkDownloadStatus(id: Long) = withContext(Dispatchers.IO) {
//        // using query method
//        // using query method
//        var finishDownload = false
//        var progress: Int
//        val downloaderManager =
//            requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
//        while (!finishDownload) {
//            val cursor: Cursor =
//                downloaderManager.query(DownloadManager.Query().setFilterById(id))
//            if (cursor.moveToFirst()) {
//                val status: Int =
//                    cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
//                when (status) {
//                    DownloadManager.STATUS_FAILED -> {
//                        finishDownload = true
//                    }
//                    DownloadManager.STATUS_PAUSED -> {
//                    }
//                    DownloadManager.STATUS_PENDING -> {
//                    }
//                    DownloadManager.STATUS_RUNNING -> {
//                        // IF we need to download a file in the future where the download happens incremently then we can use code below
////                        val total
////                            = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
////                        Log.d("Total", total.toString())
//////
////
////                         val downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
//////
////                        Log.d("downloaded", downloaded.toString())
//
////                        if (total >= 0) {
////                            val downloaded: Long =
////                                cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
////                            Log.d("Downloaded", downloaded.toString())
////                            progress = (downloaded * 100L / total).toInt()
////                            // if you use downloadmanger in async task, here you can use like this to display progress.
////                            // Don't forget to do the division in long to get more digits rather than double.
////                              updateProgress(((downloaded * 100L) / total));
////                        }
//                    }
//                    DownloadManager.STATUS_SUCCESSFUL -> {
//                        progress = 100
//                        // if you use aysnc task
//                        updateProgress(100);
//                        finishDownload = true
//                    }
//                }
//            }
//        }
//    }
//
//    private fun updateProgress(progess: Long) {
//        requireActivity().runOnUiThread {
////            Log.d("Some", progess.toString())
//            downloaderFragment_progressBar.progress = progess.toInt()
//        }
//    }




}