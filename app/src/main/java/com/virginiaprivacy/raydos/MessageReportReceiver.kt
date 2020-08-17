package com.virginiaprivacy.raydos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class MessageReportReceiver : BroadcastReceiver() {
    val eventsReceived = MutableLiveData(0)
    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.i("SMS", p1.toString())
        eventsReceived.value = eventsReceived.value?.plus(1)
    }
}