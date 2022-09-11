package com.noque.svampeatlas.view_holders

import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.models.Mushroom
import com.noque.svampeatlas.R
import com.noque.svampeatlas.models.DeterminationConfidence
import com.noque.svampeatlas.views.ResultView
import com.noque.svampeatlas.view_models.NewObservationViewModel
import kotlinx.android.synthetic.main.item_selected_result.view.*

class SelectedResultItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private var resultView: ResultView = itemView.selectedResultItem_resultView
    private var deselectButton = itemView.selectedResultItem_deSelectButton
    private var radioGroup: RadioGroup
    private var radioButtonUnsure: RadioButton
    private var radioButtonGuessing: RadioButton
    private var radioButtonDetermined: RadioButton
    private var confidenceTitleTextView: TextView
    var confidenceSet: ((DeterminationConfidence) -> Unit)? = null
    var deselectClicked: (() -> Unit)? = null


    init {
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

    fun configure(mushroom: Mushroom, confidence: DeterminationConfidence?) {
            resultView.configure(mushroom)

            deselectButton.setOnClickListener {
                deselectClicked?.invoke()
            }


            if (mushroom.isGenus) {
                radioButtonDetermined.setText(R.string.selectedSpeciesCell_confident_genus)
                radioButtonGuessing.setText(R.string.selectedSpeciesCell_likely_genus)
                radioButtonUnsure.setText(R.string.selectedSpeciesCell_possible_genus)
            } else {
                radioButtonDetermined.setText(R.string.selectedSpeciesCell_confident_species)
                radioButtonUnsure.setText(R.string.selectedSpeciesCell_likely_species)
                radioButtonGuessing.setText(R.string.selectedSpeciesCell_possible_species)
            }

            radioGroup.setOnCheckedChangeListener(null)

            when (confidence) {
                DeterminationConfidence.CONFIDENT -> { radioGroup.check(R.id.selectedResultItem_radioButton_determined) }
                DeterminationConfidence.LIKELY -> { radioGroup.check(R.id.selectedResultItem_radioButton_unsure) }
                DeterminationConfidence.POSSIBLE -> { radioGroup.check(R.id.selectedResultItem_radioButton_guessing) }
                else -> {}
            }

            radioGroup.setOnCheckedChangeListener { _, i ->
                when (i) {
                    R.id.selectedResultItem_radioButton_guessing -> {confidenceSet?.invoke(DeterminationConfidence.POSSIBLE)}
                    R.id.selectedResultItem_radioButton_unsure -> {confidenceSet?.invoke(DeterminationConfidence.LIKELY)}
                    R.id.selectedResultItem_radioButton_determined -> {confidenceSet?.invoke(DeterminationConfidence.CONFIDENT)}
                }
            }
        }
}