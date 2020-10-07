package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.drawable.*
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_searchbar.view.*
import androidx.core.content.ContextCompat.getSystemService
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.core.view.marginStart
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.noque.svampeatlas.extensions.dpToPx
import kotlinx.android.synthetic.main.item_log_out.view.*


interface SearchBarListener {
    fun newSearch(entry: String)
    fun clearedSearchEntry()
}

class SearchBarView(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    companion object {
        val TAG = "SearchBarView"
        val KEY_IS_EXPANDED = "KEY_IS_EXPANDED"
        val KEY_RECENT_SEARCH = "KEY_RECENT_SEARCH"
        val KEY_SUPER_STATE = "KEY_SUPER_STATE"
        val KEY_VISIBILITY = "KEY_VISIBILITY"
    }

    // Objects
    private val backgroundDrawable = PaintDrawable()
    private var isExpanded = true
    private var recentSearch: String? = null
    private var countDownTimer: CountDownTimer? = null

    // Views
    private var rootLayout = ConstraintSet()
    private var iconifiedLayout = ConstraintSet()

    lateinit var editText: TextInputEditText
    lateinit var textInputLayout: TextInputLayout
    lateinit var searchButton: ImageButton
    lateinit var progressBar: ProgressBar

    // Listeners

    private var listener: SearchBarListener? = null

    private val textWatcher = object: TextWatcher {
        override fun afterTextChanged(p0: Editable) {
            if (editText.hasFocus() && p0.count() > 4 && !p0.last().isWhitespace()) {
                startCountdown()
            } else {
                resetProgressbar()
            }
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    }

    private val onKeyListener = object: OnKeyListener {
        override fun onKey(view: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
            if (keyEvent?.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                onSearch()
                resignFocus()
                return true
            } else {
                return false
            }
        }
    }

    private val onClickListener = OnClickListener { searchIconPressed() }

    private val onEndIconClickListener = OnClickListener {
        listener?.clearedSearchEntry()
        editText.text = null
        recentSearch = null
        resetProgressbar()
    }

    init {
        View.inflate(getContext(), R.layout.view_searchbar, this)
        initViews()
        setupViews()
    }

    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable(KEY_SUPER_STATE, super.onSaveInstanceState())
        bundle.putBoolean(KEY_IS_EXPANDED, isExpanded)
        bundle.putString(KEY_RECENT_SEARCH, recentSearch)
        bundle.putInt(KEY_VISIBILITY, visibility)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        (state as? Bundle)?.let {
            isExpanded = it.getBoolean(KEY_IS_EXPANDED, false)
            setExpanded(isExpanded)
            recentSearch = it.getString(KEY_RECENT_SEARCH)
            visibility = it.getInt(KEY_VISIBILITY)
            it.getParcelable<Parcelable>(KEY_SUPER_STATE)?.let {
                super.onRestoreInstanceState(it)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val heightPX = MeasureSpec.getSize(heightMeasureSpec)
        progressBar.setPadding(heightPX / 2, 0, 0, 0)
        textInputLayout.setPadding(heightPX + 8.dpToPx(context), 0, 8.dpToPx(context), 0)
        backgroundDrawable.setCornerRadii(floatArrayOf((heightPX / 2).toFloat(), (heightPX / 2).toFloat(), 0F, 0F, 0F, 0F, (heightPX / 2).toFloat(), (heightPX / 2).toFloat()))
    }

    private fun initViews() {
        editText = searchBarView_editText
        textInputLayout = searchBarView_textInputLayout
        searchButton = searchBarView_button
        progressBar = searchBarView_progressBar
    }

    private fun setupViews() {
        rootLayout.clone(searchBar_root)
        iconifiedLayout.clone(context, R.layout.view_searchbar_iconified)

        editText.setOnKeyListener(onKeyListener)
        editText.addTextChangedListener(textWatcher)
        searchButton.setOnClickListener(onClickListener)
        textInputLayout.setEndIconOnClickListener(onEndIconClickListener)

        backgroundDrawable.paint.color = ResourcesCompat.getColor(resources, R.color.colorPrimary, null)
        textInputLayout.background = backgroundDrawable
    }


    private fun startCountdown() {
        val milis = 1700L
        progressBar.visibility = View.VISIBLE
        countDownTimer?.cancel()
        countDownTimer = object: CountDownTimer(milis, 10) {
            override fun onFinish() {
                onSearch()
            }

            override fun onTick(p0: Long) {
                progressBar.progress = 100 - ((p0.toDouble() / milis.toDouble()) * 100).toInt()
            }
        }

        countDownTimer?.start()
    }

    private fun resetProgressbar() {
        countDownTimer?.cancel()
        countDownTimer = null
        progressBar.visibility = View.GONE
        progressBar.progress = 0
    }

    fun setListener(listener: SearchBarListener?) {
        this.listener = listener
    }

    fun expand() {
        if (!isExpanded) {
            isExpanded = true
            setExpanded(true)
        }
    }

    fun collapse() {
        if (isExpanded) {
            isExpanded = false
            setExpanded(false)
            resetProgressbar()
        }
    }

    private fun setExpanded(isExpanded: Boolean) {
        if (isExpanded) {
            applyLayout(rootLayout)
        } else {
            resignFocus()
            applyLayout(iconifiedLayout)
        }
    }

    fun resetText() {
        editText.text = null
    }

    private fun applyLayout(layout: ConstraintSet) {
        val transition = AutoTransition()
        transition.duration = 100
        TransitionManager.beginDelayedTransition(searchBar_root, transition)
        layout.applyTo(searchBar_root)
    }

    private fun searchIconPressed() {
         if(!isExpanded) {
             expand()
             becomeFocus()
         } else {
             onSearch()
             resignFocus()
         }
        }

    private fun becomeFocus() {
        editText.requestFocus()
        val system = getSystemService(context, InputMethodManager::class.java)
            system?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)

    }

    private fun resignFocus() {
        val system = getSystemService(context, InputMethodManager::class.java)
            system?.hideSoftInputFromWindow(editText.windowToken, 0)
    }

    private fun onSearch() {
        val searchString = editText.text.toString()

        if (searchString != "" && searchString != recentSearch) {
            recentSearch = searchString
            listener?.newSearch(searchString)
        }

        resetProgressbar()
    }
}
