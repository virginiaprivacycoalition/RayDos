package com.virginiaprivacy.raydos

import java.io.Serializable

data class SavedRunningScreenData(
    val running: Boolean,
    val messagesAttempted: Int,
    val messagesSent: Int,
    val messagesDelivered: Int,
    val startTime: Long) : Serializable {
}