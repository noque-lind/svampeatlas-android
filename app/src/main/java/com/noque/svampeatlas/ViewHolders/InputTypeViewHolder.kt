package com.noque.svampeatlas.ViewHolders

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_text_input.view.*

class InputTypeViewHolder(private val onTextInputChanged: (view: View, text: String?) -> Unit, itemView: View) :
    RecyclerView.ViewHolder(itemView) {

    private lateinit var editText: EditText
    private lateinit var titleTextView: TextView

    private val textWatcher = object: TextWatcher {
        override fun afterTextChanged(p0: Editable?) {
            if (editText.hasFocus()) {
                onTextInputChanged.invoke(itemView, p0.toString())
            }
        }
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    }

    init {
        editText = itemView.textInputItem_editText
        editText.addTextChangedListener(textWatcher)
        titleTextView = itemView.textInputItem_titleTextView
    }

    fun configure(title: String, content: String?) {
        titleTextView.text = title
        editText.setText(content)
    }
}