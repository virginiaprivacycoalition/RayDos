package com.virginiaprivacy.raydos

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import com.virginiaprivacy.raydos.events.Event
import com.virginiaprivacy.raydos.events.MessageSentEvent
import com.virginiaprivacy.raydos.events.TargetNumberGeneratedEvent
import java.io.Serializable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.system.measureTimeMillis

class SmsSender(
    private val context: Context,
    private val useRandomTarget: Boolean,
    private val useRandomText: Boolean,
    private val delayMillis: Int,
    private val target: String? = null,
    private val nonRandomText: String? = null
) : Serializable {

    val smsDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    val running = AtomicBoolean(false)

    var messagesSent = 0

    val eventBus: BroadcastChannel<Event> = BroadcastChannel(Channel.BUFFERED)

    val sentIntents = CopyOnWriteArrayList<Intent>()

    private val smsManager by lazy {
        SmsManager.getDefault()
    }

    var currentTarget = 0
    var upcomingTarget = 0

    init {
        if (useRandomTarget) {
            currentTarget = getRandomNumber().toInt()
        }
    }

    suspend fun start() {
        running.set(true)
        while (running.get()) {
            withContext(Dispatchers.IO) {
                launch {
                    if (!useRandomTarget && !useRandomText) {
                        target?.let { target ->
                            nonRandomText?.let { text -> sendMessage(target, text) }
                        }
                    }
                    if (useRandomTarget && !useRandomText) {
                        awaitAll(
                            async {
                                if (nonRandomText != null) {
                                    sendMessage(currentTarget.toString(), nonRandomText)
                                }
                            }, async {
                                rotateTargetNumber()
                            })
                    }
                }
            }
            delay(delayMillis.toLong())
        }
    }

    private suspend fun rotateTargetNumber() {
        upcomingTarget = getRandomNumber().toInt()
        val event = TargetNumberGeneratedEvent(
            upcomingTarget.toString()
        )
        eventBus.send(event)
        currentTarget = upcomingTarget
    }

    fun stop() {
        running.set(false)
    }

    private fun getRandomNumber(): String {
        var number = ""
        val millis = measureTimeMillis {
            while (number.length < 9) {
                number += Random.nextInt(0..9)
            }
        }
        println("number generated in ${millis / 1000.00} seconds")
        return number
    }

    private suspend fun sendMessage(destination: String, text: String) {
        val sentIntent = PendingIntent.getBroadcast(
            context, 0, Intent(
                MESSAGE_SENT_INTENT
            ), 0
        )
        val deliveredIntent = PendingIntent.getBroadcast(
            context, 0, Intent(
                MESSAGE_DELIVERED_INTENT
            ), 0
        )
        smsManager.sendTextMessage(destination, null, text, sentIntent, deliveredIntent)
        fireSentEvent()
    }

    private suspend fun fireSentEvent() {
        eventBus.send(
            MessageSentEvent(
                messagesSent++
            )
        )
    }

    companion object {
        const val MESSAGE_SENT_INTENT = "SMS_SENT_ACTION"
        const val MESSAGE_DELIVERED_INTENT = "SMS_DELIVERED_ACTION"
    }

}



