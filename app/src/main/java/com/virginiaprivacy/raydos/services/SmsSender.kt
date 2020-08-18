package com.virginiaprivacy.raydos.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.virginiaprivacy.raydos.io.ActionType
import com.virginiaprivacy.raydos.R
import com.virginiaprivacy.raydos.io.StartRequest
import com.virginiaprivacy.raydos.events.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import java.io.Serializable
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.system.measureTimeMillis

class SmsSender : Serializable, Service()
{

    private val startTime = System.currentTimeMillis()
    private var startRequest: StartRequest? = null
    private val scope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    @RequiresApi(Build.VERSION_CODES.O)
    private val channel: NotificationChannel =
        NotificationChannel("1312", "RayDos", NotificationManager.IMPORTANCE_DEFAULT)


    private fun getNotification() = NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.fist)
        .setContentTitle(getString(R.string.notification_title))
        .setContentText("Running")
        .setChannelId("1312")
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)!!


    private val running = AtomicBoolean(false)

    var messagesSent = 0

    private val smsManager by lazy {
        SmsManager.getDefault()
    }

    private var currentTarget = 0
        set(value)
        {
            field = value
            scope.launch { eventBus.send(TargetNumberGeneratedEvent(value.toString())) }
        }
    private var upcomingTarget = 0
        set(value)
        {
            field = value
            scope.launch { eventBus.send(TargetNumberGeneratedEvent(value.toString())) }
        }
    private var currentMessageText = ""
        set(value)
        {
            field = value
            scope.launch { eventBus.send(TextGeneratedEvent(value)) }
        }
    private var delay = 1000


    override fun onBind(p0: Intent?): IBinder?
    {
        return null
    }

    override fun onCreate()
    {
        super.onCreate()
        with(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                this.createNotificationChannel(channel)
            }
        }
        startForeground(RAYDOS_NOTIFICATION_ID, getNotification().build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        intent?.let {
            if (it.action == ActionType.START_SERVICE)
            {
                if (it.getSerializableExtra("start_request") is StartRequest)
                {
                    this.startRequest = it.getSerializableExtra("start_request") as StartRequest
                    if (!running.get())
                    {
                        if (startRequest?.useRandomTarget!!)
                        {
                            currentTarget = getRandomNumber().toInt()
                        }
                        if (!startRequest?.useRandomText!!)
                        {
                            currentMessageText = startRequest?.nonRandomText.toString()
                        }
                        scope.launch {
                            start()
                        }
                    }
                }
            }
            if (it.action == ActionType.STOP_SERVICE)
            {
                stopSelf(startId)
                running.set(false)
                cancel()
            }
        }
        return START_STICKY
    }

    override fun onDestroy()
    {
        super.onDestroy()
        running.set(false)
        scope.cancel()
    }


    private suspend fun start()
    {
        if (fireStartEvent()) return
        if (startRequest != null)
        {
            currentTarget = when (startRequest!!.useRandomTarget)
            {
                true -> getRandomNumber().toInt()
                else -> startRequest?.target!!.toInt()
            }
            currentMessageText = when (startRequest!!.useRandomText)
            {
                true -> TODO("Random String generation fun")
                else -> startRequest?.nonRandomText!!
            }
            delay = if (startRequest?.delayMillis != null) startRequest?.delayMillis!! else delay
        }
        while (running.get())
        {
            scope.launch {
                sendMessage(currentTarget.toString(), currentMessageText)
                if (startRequest?.useRandomTarget!!)
                {
                    currentTarget = getRandomNumber().toInt()
                }
                if (startRequest?.useRandomText!!)
                {
                    currentMessageText = "this is the current message text"
                }
            }
            delay(startRequest!!.delayMillis.toLong())
        }
    }


    private suspend fun fireStartEvent(): Boolean
    {
        if (running.get())
        {
            Log.i("1312", "Service already running!")
            return true
        }
        running.set(true)
        eventBus.send(ServiceStartedEvent(startTime, getNotification()))
        return false
    }


    private suspend fun rotateTargetNumber()
    {
        upcomingTarget = getRandomNumber().toInt()
        val event = TargetNumberGeneratedEvent(
            upcomingTarget.toString()
        )
        eventBus.send(event)
        currentTarget = upcomingTarget
    }

    private fun cancel()
    {
        running.set(false)
        Log.d(this::class.qualifiedName, "Stopping service $this")
        stopSelf()
        scope.cancel()
    }

    private fun runtime(): String
    {

        val runtime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            Duration.of(System.currentTimeMillis(), ChronoUnit.MILLIS)
        }
        else
        {
            val dur = System.currentTimeMillis() - startTime
            var seconds = dur / 1000.0
            var minutes = seconds / 60
            var hours = minutes / 60
            if (hours >= 1)
            {
                minutes -= (60 * hours)
            }
            else
            {
                hours = 0.0
            }
            if (minutes >= 1)
            {
                seconds -= (60 * minutes)
            }
            else
            {
                minutes = 0.0
            }
            return "${hours}:$minutes:$seconds"
        }
        return "${runtime.toHours()}:${runtime.toMinutes()}:${runtime.seconds}"

    }

    private fun getRandomNumber(): String
    {
        var number = ""
        val millis = measureTimeMillis {
            while (number.length < 9)
            {
                number += Random.nextInt(0..9)
            }
        }
        Log.d("SmsSender", "number generated in ${millis / 1000.00} seconds")
        return number
    }

    private suspend fun sendMessage(destination: String, text: String)
    {
        val sentIntent = PendingIntent.getBroadcast(
            applicationContext, 0, Intent(
                MESSAGE_SENT_INTENT
            ), 0
        )
        val deliveredIntent = PendingIntent.getBroadcast(
            applicationContext, 0, Intent(
                MESSAGE_DELIVERED_INTENT
            ), 0
        )

        smsManager.sendTextMessage(destination, null, text, sentIntent, deliveredIntent)
        fireSentEvent()
    }


    private suspend fun fireSentEvent()
    {
        eventBus.send(
            MessageSentEvent(
                messagesSent++
            )
        )
    }

    companion object
    {
        const val MESSAGE_SENT_INTENT = "SMS_SENT_ACTION"
        const val MESSAGE_DELIVERED_INTENT = "SMS_DELIVERED_ACTION"
        const val RAYDOS_NOTIFICATION_ID = 11913
        val eventBus: BroadcastChannel<Event> = BroadcastChannel(Channel.BUFFERED)
    }


}




