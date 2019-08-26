package com.noque.svampeatlas.ViewHolders

import android.util.Log
import android.view.View
import android.widget.RadioGroup
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Model.Mushroom
import com.noque.svampeatlas.R
import com.noque.svampeatlas.View.Views.ResultView
import com.noque.svampeatlas.ViewModel.NewObservationViewModel
import kotlinx.android.synthetic.main.item_selected_result.view.*

class SelectedResultItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var resultView: ResultView
    private var radioGroup: RadioGroup
    var confidenceSet: ((NewObservationViewModel.DeterminationConfidence) -> Unit)? = null


    init {
        resultView = itemView.selectedResultItem_resultView
        radioGroup = itemView.selectedResultItem_confidenceRadioButtonGroup
    }

    fun setOnClickListener(listener: View.OnClickListener) {
        resultView.tag = this
        resultView.setOnClickListener(listener)
    }

    fun configure(mushroom: Mushroom, confidence: NewObservationViewModel.DeterminationConfidence?) {
        Log.d("ViewModel", confidence.toString())

        resultView.configure(mushroom)
        radioGroup.setOnCheckedChangeListener(null)

        when (confidence) {
            NewObservationViewModel.DeterminationConfidence.CONFIDENT -> { radioGroup.check(R.id.selectedResultItem_radioButton_determined) }
            NewObservationViewModel.DeterminationConfidence.LIKELY -> { radioGroup.check(R.id.selectedResultItem_radioButton_unsure) }
            NewObservationViewModel.DeterminationConfidence.POSSIBLE -> { radioGroup.check(R.id.selectedResultItem_radioButton_guessing) }
        }

        radioGroup.setOnCheckedChangeListener { radioGroup, i ->
            when (i) {
                R.id.selectedResultItem_radioButton_guessing -> {confidenceSet?.invoke(NewObservationViewModel.DeterminationConfidence.POSSIBLE)}
                R.id.selectedResultItem_radioButton_unsure -> {confidenceSet?.invoke(NewObservationViewModel.DeterminationConfidence.LIKELY)}
                R.id.selectedResultItem_radioButton_determined -> {confidenceSet?.invoke(NewObservationViewModel.DeterminationConfidence.CONFIDENT)}
            }
        }
    }
}