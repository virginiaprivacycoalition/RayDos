package com.virginiaprivacy.raydos.io

import com.virginiaprivacy.raydos.services.SmsSender
import java.io.Serializable

data class StartRequest(
    val useRandomTarget: Boolean,
    val useRandomText: Boolean,
    val delayMillis: Int,
    val target: String? = null,
    val nonRandomText: String? = null,
    val customSmsSource: Boolean = false,
    val customSourceTarget: String? = null,
    val logPhoneState: Boolean = false
) : Serializable {
    companion object {
        fun from(sender: SmsSender): StartRequest {
            return StartRequest(
                sender.startRequest!!.useRandomTarget,
                sender.startRequest!!.useRandomText,
                sender.startRequest!!.delayMillis,
                sender.currentTarget,
                sender.startRequest!!.nonRandomText,
                sender.useSourceField,
                sender.currentSource,
                sender.logPhoneState
            )
        }
    }
}

interface ActionType
{
    companion object
    {
        const val START_SERVICE = "start_service"
        const val STOP_SERVICE = "stop_service"
        const val RESUME_UI = "resume_ui"


    }
}