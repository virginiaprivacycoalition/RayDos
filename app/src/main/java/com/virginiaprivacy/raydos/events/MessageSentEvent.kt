package com.virginiaprivacy.raydos.events

import com.virginiaprivacy.raydos.events.Event

data class MessageSentEvent(val messageNumber: Int) :
    Event {
}