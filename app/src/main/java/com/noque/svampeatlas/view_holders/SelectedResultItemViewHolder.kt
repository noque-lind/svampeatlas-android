package com.noque.svampeatlas.view_holders

import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.R
import com.noque.svampeatlas.views.ResultView
import com.noque.svampeatlas.view_models.NewObservationViewModel
import kotlinx.android.synthetic.main.item_selected_result.view.*

class SelectedResultItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var resultView: ResultView
    private var radioGroup: RadioGroup
    private var radioButtonUnsure: RadioButton
    private var radioButtonGuessing: RadioButton
    private var radioButtonDetermined: RadioButton
    private var confidenceTitleTextView: TextView
    var confidenceSet: ((NewObservationViewModel.DeterminationConfidence) -> Unit)? = null


    init {
        resultView = itemView.selectedResultItem_resultView
        radioGroup = itemView.selectedResultItem_confidenceRadioButtonGroup
        radioButtonDetermined = itemView.selectedResultItem_radioButton_determined
        radioButtonUnsure = itemView.selectedResultItem_radioButton_unsure
        radioButtonGuessing = itemView.selectedResultItem_radioButton_guessing

        confidenceTitleTextView = itemView.selectedResultItem_confidenceTitleTextView
    }

    fun setOnClickListener(listener: View.OnClickListener) {
        resultView.tag = this
        resultView.setOnClickListener(listener)
    }

    fun configure(mushroom: Mushroom, confidence: NewObservationViewModel.DeterminationConfidence?) {
            resultView.configure(mushroom)

            if (mushroom.isGenus) {
                radioButtonDetermined.setText(R.string.genus_confidence_determined)
                radioButtonGuessing.setText(R.string.genus_confidence_guessing)
                radioButtonUnsure.setText(R.string.genus_confidence_unsure)
            } else {
                radioButtonDetermined.setText(R.string.species_confidence_determined)
                radioButtonUnsure.setText(R.string.species_confidence_unsure)
                radioButtonGuessing.setText(R.string.species_confidence_guessing)
            }

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