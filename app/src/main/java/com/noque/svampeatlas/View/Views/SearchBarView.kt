package com.noque.svampeatlas.View.Views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.*
import android.graphics.drawable.shapes.Shape
import android.icu.util.Measure
import android.text.TextPaint
import android.transition.AutoTransition
import android.transition.Transition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setPadding
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.view_searchbar.view.*
import androidx.core.content.ContextCompat.getSystemService
import android.view.inputmethod.InputMethod
import android.view.inputmethod.InputMethodManager
import androidx.core.util.toHalf


interface SearchBarListener {
    fun newSearch(entry: String)
    fun clearedSearchEntry()
}

class SearchBarView(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private var rootLayout = ConstraintSet()
    private var iconifiedLayout = ConstraintSet()
    private var listener: SearchBarListener? = null
    private var isExpanded = true
    private var recentSearch: String? = null
    private val backgroundDrawable = PaintDrawable()

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

    private val onClickListener = object: OnClickListener {
        override fun onClick(view: View?) {
            searchIconPressed()
        }

    }

    init {
        View.inflate(getContext(), R.layout.view_searchbar, this)
        setupView()
    }

    private fun setupView() {
        rootLayout.clone(searchBar_root)
        iconifiedLayout.clone(context, R.layout.view_searchbar_iconified)

        searchBar_editText.setOnKeyListener(onKeyListener)
        searchBar_button.setOnClickListener(onClickListener)


        backgroundDrawable.paint.color = ResourcesCompat.getColor(resources, R.color.colorPrimary, null)
        searchBar_editText.setBackground(backgroundDrawable)
    }



    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val heightPX = MeasureSpec.getSize(heightMeasureSpec)
        searchBar_editText.setPadding(heightPX + dpToPx(8), 0, dpToPx(8), 0)
        backgroundDrawable.setCornerRadii(floatArrayOf((heightPX / 2).toFloat(), (heightPX / 2).toFloat(), 0F, 0F, 0F, 0F, (heightPX / 2).toFloat(), (heightPX / 2).toFloat()))
    }

    fun setListener(listener: SearchBarListener?) {
        this.listener = listener
    }

    fun expand() {
        if (!isExpanded) {
            isExpanded = true
            applyLayout(rootLayout)
        }
    }

    fun collapse() {
        if (isExpanded) {
            isExpanded = false
            resignFocus()
            applyLayout(iconifiedLayout)
        }
    }

    fun resetText() {
        searchBar_editText.text = null
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
        searchBar_editText.requestFocus()
        val system = getSystemService(context, InputMethodManager::class.java)
        system?.let {
            it.showSoftInput(searchBar_editText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun resignFocus() {
        val system = getSystemService(context, InputMethodManager::class.java)
        system?.let {
            it.hideSoftInputFromWindow(searchBar_editText.windowToken, 0)
        }
    }

    private fun onSearch() {
        resignFocus()

        val searchString = searchBar_editText.text.toString()

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

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density.toInt())
    }

    private fun pxToDp(px: Int): Int {
        return (px / context.resources.displayMetrics.density.toInt())
    }
}
