package com.noque.svampeatlas.fragments

import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.fragment_prompt.*

class PromptFragment: DialogFragment() {


    companion object {
        const val KEY_TITLE = "KEY_TITLE"
        const val KEY_MESSAGE = "KEY_MESSAGE"
        const val KEY_POSITIVE = "KEY_POSITIVE"
        const val KEY_NEGATIVE = "KEY_NEGATIVE"
    }

    interface Listener {
        fun positiveButtonPressed()
        fun negativeButtonPressed()
    }

// Views
    private lateinit var titleTextView: TextView
    private lateinit var messageTextView: TextView
    private lateinit var positiveButton: Button
    private lateinit var negativeButton: Button


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_prompt, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupViews()
    }

    private fun initViews() {
        titleTextView = promptFragment_titleTextView
        messageTextView = promptFragment_messageTextView
        positiveButton = promptFragment_positiveButton
        negativeButton = promptFragment_negativeButton
    }

    private fun setupViews() {
        titleTextView.text = arguments?.getString(KEY_TITLE)
        messageTextView.text = arguments?.getString(KEY_MESSAGE)
        positiveButton.text = arguments?.getString(KEY_POSITIVE)
        negativeButton.text = arguments?.getString(KEY_NEGATIVE)

        positiveButton.setOnClickListener {
            (targetFragment as Listener).positiveButtonPressed()
            dismiss()
        }
        negativeButton.setOnClickListener {
            (targetFragment as Listener).negativeButtonPressed()
            dismiss()
        }
    }
}