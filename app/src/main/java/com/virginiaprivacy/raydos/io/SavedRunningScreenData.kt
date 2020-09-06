package com.virginiaprivacy.raydos.io

import java.io.Serializable

data class SavedRunningScreenData(
    val running: Boolean,
    val messagesAttempted: Int,
    val messagesDelivered: Int,
    val messagesSent: Int,
    val startTime: Long
) : Serializable