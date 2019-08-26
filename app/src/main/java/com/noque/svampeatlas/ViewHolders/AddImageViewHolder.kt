package com.noque.svampeatlas.ViewHolders

import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import kotlinx.android.synthetic.main.item_add_image.view.addImageItem_root

class AddImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

//    private var rootLayout = ConstraintSet()
//    private var collapsedLayout = ConstraintSet()
//    private var isCollapsed: Boolean = false

//
//    init {
//        rootLayout.clone(itemView.context, R.layout.item_add_image)
//        collapsedLayout.clone(itemView.context, R.layout.item_add_image_expanded)
//       val layout =  itemView.addImageItem_root
//        val params = layout.layoutParams
//        params.height = LinearLayout.LayoutParams.WRAP_CONTENT
//        layout.layoutParams = params
//    }
//
//    fun collapse() {
//        if (isCollapsed) return
//        Log.d("Holder", "Should collapse")
//        applyLayout(collapsedLayout)
//        isCollapsed = true
//    }
//
//    fun expand() {
//        if (!isCollapsed) return
//        Log.d("Holder", "Should expand")
//        applyLayout(rootLayout)
//        isCollapsed = false
//    }
//
//    private fun applyLayout(layout: ConstraintSet) {
////        val transition = AutoTransition()
////        transition.duration = 100
////        TransitionManager.beginDelayedTransition(itemView.addImageItem_root, transition)
//        layout.applyTo(itemView.addImageItem_root)
//    }

}