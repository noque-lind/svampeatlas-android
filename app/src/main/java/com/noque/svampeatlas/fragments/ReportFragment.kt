package com.noque.svampeatlas.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import androidx.core.view.marginStart
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import com.noque.svampeatlas.R
import com.noque.svampeatlas.extensions.dpToPx
import com.noque.svampeatlas.view_models.Session

class ReportFragment(private val observationID: Int): DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val editText = EditText(requireContext())
        editText.inputType = InputType.TYPE_CLASS_TEXT

        val dialog = AlertDialog.Builder(requireContext())
            .setView(editText)
            .setMessage(R.string.observationDetailsScrollView_rapportContent_message)
            .setCancelable(true)
            .setNegativeButton("OK") { _, _ ->
                Session.postOffensiveContentComment(observationID, editText.text.toString())
                dismiss()
            }

            .setNeutralButton(R.string.observationDetailsScrollView_rapportContent_abort) { _, _ ->
                dismiss()
            }

        return dialog.create()

    }

}