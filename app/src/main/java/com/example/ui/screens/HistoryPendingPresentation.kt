package com.example.ui.screens

import com.example.model.OrderStatus
import com.example.model.OrderHistoryItem

/** Pending rows stay in All History; temporary repository reasons are never presentation. */
object HistoryPendingPresentation {
    const val LABEL = "Pending"
    fun forDisplay(item: OrderHistoryItem): OrderHistoryItem = if (item.status == OrderStatus.PROCESSING)
        item.copy(reason = "", decisionReasonText = "") else item
}
