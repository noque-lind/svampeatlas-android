package com.noque.svampeatlas.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.loadGif
import com.noque.svampeatlas.fragments.add_observation.DetailsPickerFragment
import com.noque.svampeatlas.utilities.SharedPreferences
import com.noque.svampeatlas.views.HeaderView
import kotlinx.android.synthetic.main.fragment_details_picker.*
import kotlinx.android.synthetic.main.fragment_terms.*

class TermsFragment: DialogFragment() {

    companion object {
        const val KEY_TYPE = "KEY_TERMFRAGMENT_TYPE"
    }

    enum class Type {
        IDENTIFICATION,
        LOCALITYHELPER,
        CAMERAHELPER,
        WHATSNEW,
        IMAGEDELETIONS
    }

    interface Listener {
        fun onDismiss(termsAccepted: Boolean)
    }


    private lateinit var type: Type
    var listener: Listener? = null


    // Views

    private lateinit var headerView: HeaderView
    private lateinit var contentTextView: TextView
    private lateinit var acceptButton: Button
    private lateinit var imageView: ImageView


    // Listeners

    private val acceptButtonPressed by lazy {
        View.OnClickListener {
            when (type) {
                Type.IDENTIFICATION -> {
                    SharedPreferences.setHasAcceptedIdentificationTerms(true)
                }
                Type.IMAGEDELETIONS -> {
                    SharedPreferences.hasSeenImageDeletion = true
                }
                Type.WHATSNEW -> {
                    SharedPreferences.hasSeenWhatsNew = true
                }
                Type.LOCALITYHELPER -> {
                    SharedPreferences.setHasShownPositionReminder()
                }
                else -> {}
            }

            listener?.onDismiss(termsAccepted = true)
            dismiss()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_terms, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        type = arguments?.getSerializable(KEY_TYPE) as Type

        initViews()
        setupViews()
    }


    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        val height = (resources.displayMetrics.heightPixels * 0.70).toInt()
        dialog?.window?.setLayout(width, height)
    }

    private fun initViews() {
        imageView = termsFragment_imageView
        contentTextView = termsFragment_contentTextView
        acceptButton = termsFragment_acceptButton
        headerView = termsFragment_headerView
    }

    private fun setupViews() {
        acceptButton.setOnClickListener(acceptButtonPressed)

        when (type) {
            Type.IDENTIFICATION -> {
                headerView.configure(getString(R.string.termsVC_mlPredict_title))
                contentTextView.text = getString(R.string.termsVC_mlPredict_message)
            }
            Type.CAMERAHELPER -> {
                headerView.configure(getString(R.string.termsVC_cameraHelper_title))
                contentTextView.text = getString(R.string.termsVC_cameraHelper_message)
            }
            Type.WHATSNEW -> {
                headerView.configure(getString(R.string.whats_new_title))
                contentTextView.text = getString(R.string.whats_new_3_0)
            }
            Type.IMAGEDELETIONS -> {
                headerView.configure(getString(R.string.message_deletionsAreFinal))
                contentTextView.text = getString(R.string.message_imageDeletetionsPermanent)
            }
            Type.LOCALITYHELPER -> {
                imageView.loadGif(R.drawable.locality_helper)
                headerView.configure(resources.getString(R.string.modal_localityHelper_title))
                contentTextView.text = resources.getString(R.string.modal_localityHelper_message)
            }
        }
    }
}