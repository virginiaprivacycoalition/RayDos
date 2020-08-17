package com.virginiaprivacy.raydos.events

import androidx.core.app.NotificationCompat

data class ServiceStartedEvent(val startTime: Long, val notification: NotificationCompat.Builder) : Event {
}