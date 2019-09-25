package com.noque.svampeatlas.view_holders

import android.text.SpannableStringBuilder
import android.view.View
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.noque.svampeatlas.extensions.Date
import com.noque.svampeatlas.extensions.highlighted
import com.noque.svampeatlas.extensions.italized
import com.noque.svampeatlas.extensions.toReadableDate
import com.noque.svampeatlas.models.Notification
import com.noque.svampeatlas.services.DataService
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
                profileImageView.configure(notification.triggerInitials, notification.triggerImageURL, DataService.ImageSize.FULL)
                s.append(" har kommenteret på et fund af: ")
                s.bold { append(notification.observationFullName.italized()) }
                s.append(" som du følger.")
            }

            "DETERMINATION_ADDED" -> {
                profileImageView.configure(notification.triggerInitials, notification.triggerImageURL, DataService.ImageSize.FULL)
                s.append("har tilføjet bestemmelsen: ")
                s.bold { append(notification.observationFullName.italized()) }
                s.append(" til et fund som du følger.")
            }

            "DETERMINATION_APPROVED" -> {
                profileImageView.configure(null, notification.imageURL, DataService.ImageSize.MINI)
                s.clear()
                s.append("Et fund du følger er blevet valideret og godkendt som: ")
                s.bold { append(notification.observationFullName.italized()) }
            }

            "DETERMINATION_EXPERT_APPROVED" -> {
                profileImageView.configure(null, notification.imageURL, DataService.ImageSize.MINI)
                s.clear()
                s.append("Fundet af: ")
                s.bold { append(notification.observationFullName.italized()) }
                s.append(" er blevet ekspertgodkendt")
            }
        }

        primaryLabel.text = s
        secondaryLabel.text = Date(notification.date)?.toReadableDate()
    }
}