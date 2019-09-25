package com.noque.svampeatlas.views

import android.content.Context
import android.graphics.drawable.*
import android.os.Bundle
import android.os.Parcelable
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.AttributeSet
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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.noque.svampeatlas.extensions.dpToPx


interface SearchBarListener {
    fun newSearch(entry: String)
    fun clearedSearchEntry()
}

class SearchBarView(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    companion object {
        val KEY_IS_EXPANDED = "KEY_IS_EXPANDED"
        val KEY_RECENT_SEARCH = "KEY_RECENT_SEARCH"
        val KEY_SUPER_STATE = "KEY_SUPER_STATE"
        val KEY_VISIBILITY = "KEY_VISIBILITY"
    }

    // Objects
    private val backgroundDrawable = PaintDrawable()
    private var isExpanded = true
    private var recentSearch: String? = null


    // Views
    private var rootLayout = ConstraintSet()
    private var iconifiedLayout = ConstraintSet()

    lateinit var editText: TextInputEditText
    lateinit var textInputLayout: TextInputLayout
    lateinit var searchButton: ImageButton

    // Listeners

    private var listener: SearchBarListener? = null

    private val onKeyListener = object: OnKeyListener {
        override fun onKey(view: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
            if (keyEvent?.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                onSearch()
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

    private fun initViews() {
        editText = searchBarView_editText
        textInputLayout = searchBarView_textInputLayout
        searchButton = searchBarView_button
    }

    private fun setupViews() {
        rootLayout.clone(searchBar_root)
        iconifiedLayout.clone(context, R.layout.view_searchbar_iconified)

        editText.setOnKeyListener(onKeyListener)
        searchButton.setOnClickListener(onClickListener)
        textInputLayout.setEndIconOnClickListener(onEndIconClickListener)

        backgroundDrawable.paint.color = ResourcesCompat.getColor(resources, R.color.colorPrimary, null)
        textInputLayout.background = backgroundDrawable
    }



    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val heightPX = MeasureSpec.getSize(heightMeasureSpec)
        textInputLayout.setPadding(heightPX + 8.dpToPx(context), 0, 8.dpToPx(context), 0)
        backgroundDrawable.setCornerRadii(floatArrayOf((heightPX / 2).toFloat(), (heightPX / 2).toFloat(), 0F, 0F, 0F, 0F, (heightPX / 2).toFloat(), (heightPX / 2).toFloat()))
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
         if(isExpanded) {
             collapse()
             listener?.clearedSearchEntry()
         } else {
             expand()
             becomeFocus()
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
        resignFocus()

        val searchString = editText.text.toString()

        if (searchString != "" && searchString != recentSearch) {
            recentSearch = searchString
            listener?.newSearch(searchString)
        }

        // MAKE ANIMATION TO BECOME BIGGER


//
//        guard let entry = text, entry != recentSearch else {return}
//        searchBarDelegate?.newSearchEntry(entry: entry)
//        recentSearch = entry
//
//        UIView.animate(withDuration: 0.1, animations: {
//            self.transform = CGAffineTransform(scaleX: 1.1, y: 1.1)
//        }) { (_) in
//                UIView.animate(withDuration: 0.1, animations: {
//            self.transform = CGAffineTransform.identity
//        })
    }
}
