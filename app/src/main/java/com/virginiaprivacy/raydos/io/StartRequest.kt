package com.virginiaprivacy.raydos.io

import android.content.Context
import java.io.Serializable

data class StartRequest(
    val useRandomTarget: Boolean,
    val useRandomText: Boolean,
    val delayMillis: Int,
    val target: String? = null,
    val nonRandomText: String? = null
) : Serializable

interface ActionType
{
    companion object
    {
        const val START_SERVICE = "start_service"
        const val STOP_SERVICE = "stop_service"
    }
}