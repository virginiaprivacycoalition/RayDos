package com.virginiaprivacy.raydos.events

import com.virginiaprivacy.raydos.events.Event

data class TargetNumberGeneratedEvent(val destination: String) :
    Event {
}