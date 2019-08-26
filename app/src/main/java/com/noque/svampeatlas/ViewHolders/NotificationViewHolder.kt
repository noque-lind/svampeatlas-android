package com.noque.svampeatlas.ViewHolders

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.Extensions.Date
import com.noque.svampeatlas.Extensions.highlighted
import com.noque.svampeatlas.Extensions.italized
import com.noque.svampeatlas.Extensions.toReadableDate
import com.noque.svampeatlas.Model.Notification
import kotlinx.android.synthetic.main.item_notification.view.*

class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val profileImageView = itemView.notificationItem_profileImageView
    private val primaryLabel = itemView.notificationItem_primaryLabel
    private val secondaryLabel = itemView.notificationItem_secondaryLabel


    fun configure(notification: Notification) {
        var s: SpannableStringBuilder
        s = notification.triggerName.highlighted()

        when (notification.eventType) {
            "COMMENT_ADDED" -> {
                profileImageView.configure(notification.triggerInitials, notification.triggerImageURL)
                s.append(" har kommenteret på et fund af: ")
                s.bold { append(notification.observationFullName.italized()) }
                s.append(" som du følger.")
            }

            "DETERMINATION_ADDED" -> {
                profileImageView.configure(notification.triggerInitials, notification.triggerImageURL)
                s.append("har tilføjet bestemmelsen: ")
                s.bold { append(notification.observationFullName.italized()) }
                s.append(" til et fund som du følger.")
            }

            "DETERMINATION_APPROVED" -> {
                profileImageView.configure(null, notification.imageURL)
                s.clear()
                s.append("Et fund du følger er blevet valideret og godkendt som: ")
                s.bold { append(notification.observationFullName.italized()) }
            }

            "DETERMINATION_EXPERT_APPROVED" -> {
                profileImageView.configure(null, notification.imageURL)
                s.clear()
                s.append("Fundet af: ")
                s.bold { append(notification.observationFullName.italized()) }
                s.append(" er blevet ekspertgodkendt")
            }
        }

        primaryLabel.text = s
        secondaryLabel.text = Date(notification.date).toString()
    }
}