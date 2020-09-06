package com.virginiaprivacy.raydos.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.virginiaprivacy.raydos.MainActivity
import com.virginiaprivacy.raydos.R
import com.virginiaprivacy.raydos.events.*
import com.virginiaprivacy.raydos.io.ActionType
import com.virginiaprivacy.raydos.io.StartRequest
import com.virginiaprivacy.raydos.models.TextMessage
import com.virginiaprivacy.raydos.settings.SettingsActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.Serializable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.system.measureTimeMillis

@ExperimentalCoroutinesApi
class SmsSender : Serializable, Service() {

    var startRequest: StartRequest? = null
    var logPhoneState = false
    var useSourceField = false
    var currentSource = ""
        set(value) {
            field = value
            scope.launch { eventBus.send(SourceNumberGenerated(value)) }
        }

    private var areaCode = ""
    
    private val startTime = System.currentTimeMillis()
    private val scope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    private val running = AtomicBoolean(false)
    private var delay = 1000
    private var messagesSent = 0
    private val notificationChannelId by lazy { getString(R.string.notification_channel_id) }

    private val smsManager by lazy {
        SmsManager.getDefault()
    }

    private val textGenerator by lazy {
        Json.decodeFromString(
            ListSerializer(TextMessage.serializer()),
            assets.open("TextMessages.json").readBytes().decodeToString()
        )
    }

    private val channel: NotificationChannel by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                notificationChannelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT)
        }
        else {
            TODO("VERSION.SDK_INT < O")
        }
    }

    private val notification by lazy {
        NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.drawable.running_icon)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_content))
            .setChannelId(notificationChannelId)
            .setUsesChronometer(true)
            .setWhen(startTime)
            .addAction(R.drawable.info_icon,
                getString(R.string.info_notification_text),
                TaskStackBuilder.create(this).run {
                    addNextIntentWithParentStack(Intent(this@SmsSender, MainActivity::class.java)
                        .apply {
                            action = ActionType.RESUME_UI
                            putExtra("saved", StartRequest.from(this@SmsSender))
                        })
                        .getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT)
                })
            .setOngoing(true)
//        .addAction(R.drawable.exit_icon, getString(R.string.exit_notification_text), PendingIntent.getService(applicationContext,
//            1, Intent(ActionType.STOP_SERVICE), PendingIntent.FLAG_CANCEL_CURRENT))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)!!
    }

    @ExperimentalCoroutinesApi
    var currentTarget: String = ""
        set(value) {
            field = value
            scope.launch { eventBus.send(TargetNumberGeneratedEvent(value)) }
        }

    @ExperimentalCoroutinesApi
    private var currentMessageText = ""
        set(value) {
            field = value
            scope.launch { eventBus.send(TextGeneratedEvent(value)) }
        }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        with(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.createNotificationChannel(channel)
            }
        }


    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.action == ActionType.START_SERVICE) {
                if (it.getSerializableExtra("start_request") is StartRequest) {
                    this.startRequest = it.getSerializableExtra("start_request") as StartRequest
                    if (!running.get()) {
                        if (startRequest?.useRandomTarget!!) {
                            val savedAreaCode =
                                MainActivity.prefs.pull(SettingsActivity.KEY_PREF_AREA_CODE, "")
                            if (savedAreaCode.length == 3) {
                                areaCode = savedAreaCode
                            }
                            currentTarget = getRandomNumber()
                        }
                        if (!startRequest?.useRandomText!!) {
                            currentMessageText = startRequest?.nonRandomText.toString()
                        }
                        else {
                            scope.launch {
                                currentMessageText = textGenerator.random().content
                            }
                        }
                        startRequest?.customSmsSource.let { use ->
                            if (use != null) {
                                useSourceField = use
                                currentSource = startRequest?.customSourceTarget.toString()
                            }
                        }
                        val notification = notification.build()
                        startForeground(RAYDOS_NOTIFICATION_ID, notification)
                        scope.launch {
                            start()
                        }
                    }
                }
            }
            if (it.action == ActionType.STOP_SERVICE) {
                stopSelf(startId)
                running.set(false)
                cancel()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        running.set(false)
        scope.cancel()
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    private suspend fun start() {
        if (fireStartEvent()) return

        scope.launch {
            updateNotificationDescription()
        }

        if (startRequest != null) {
            logPhoneState = startRequest!!.logPhoneState
            currentTarget = when (startRequest!!.useRandomTarget) {
                true -> getRandomNumber()
                else -> startRequest?.target!!
            }
            currentMessageText = when (startRequest!!.useRandomText) {
                true -> textGenerator.random().content
                else -> startRequest?.nonRandomText!!
            }
            delay = if (startRequest?.delayMillis != null) startRequest?.delayMillis!! else delay
        }
        while (running.get()) {
            scope.launch {
                sendMessage(currentTarget, currentMessageText)
                if (startRequest?.useRandomTarget!!) {
                    currentTarget = getRandomNumber()
                }
                if (startRequest?.useRandomText!!) {
                    scope.launch {
                        currentMessageText = textGenerator.random().content
                    }
                }
                if (startRequest?.customSmsSource!!) {
                    currentSource = getRandomNumber()
                }
            }
            delay(startRequest!!.delayMillis.toLong())
        }
    }

    @FlowPreview
    private suspend fun updateNotificationDescription() {
        listenByEventType<MessageSentEvent> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel.description = "RayDos has sent ${this.messageNumber} messages this session."
            }
        }
    }

    @ExperimentalCoroutinesApi
    private suspend fun fireStartEvent(): Boolean {
        if (running.get()) {
            Log.i("1312", "Service already running!")
            return true
        }
        running.set(true)
        eventBus.send(ServiceStartedEvent(startTime, notification))
        return false
    }

    private fun cancel() {
        running.set(false)
        Log.d(this::class.qualifiedName, "Stopping service $this")
        stopSelf()
        scope.cancel()
    }

    private fun getRandomNumber(): String {
        var number = ""
        if (areaCode.length == 3) {
            number += areaCode
        }
        val millis = measureTimeMillis {
            while (number.length < 9) {
                number += Random.nextInt(0..9)
            }
        }
        Log.v("SmsSender", "number generated in ${millis / 1000.00} seconds")
        return number
    }

    @ExperimentalCoroutinesApi
    private suspend fun sendMessage(destination: String, text: String) {
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
        val src: String? = when (useSourceField) {
            true -> currentSource
            else -> null
        }

        smsManager.sendTextMessage(destination, src, text, sentIntent, deliveredIntent)
        fireSentEvent()
    }


    @ExperimentalCoroutinesApi
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
        const val RAYDOS_NOTIFICATION_ID = 11913

        @ExperimentalCoroutinesApi
        val eventBus: BroadcastChannel<Event> = BroadcastChannel(Channel.BUFFERED)
    }
}

@FlowPreview
@ExperimentalCoroutinesApi
suspend inline fun <reified T> listenByEventType(crossinline run: T.() -> Unit) {
    return SmsSender.eventBus.asFlow().filterIsInstance<T>().collect { it.run() }
}
