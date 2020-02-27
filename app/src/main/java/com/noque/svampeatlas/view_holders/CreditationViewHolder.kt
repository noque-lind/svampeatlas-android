package com.noque.svampeatlas.view_holders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.AppError
import kotlinx.android.synthetic.main.item_creditation.view.*
import kotlinx.android.synthetic.main.item_error.view.*

class CreditationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    enum class Type {
        AI,
        AINEWOBSERVATION
    }

    private val textView = itemView.item_creditation_textView

    fun configure(type: Type) {
        when (type) {
            Type.AI -> textView.setText(R.string.creditationCell_ai)
            Type.AINEWOBSERVATION -> textView.setText(R.string.creditationCell_aiNewObservation)
        }
    }
}