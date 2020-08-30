package com.virginiaprivacy.raydos.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.virginiaprivacy.raydos.MainActivity
import com.virginiaprivacy.raydos.R
import com.virginiaprivacy.raydos.events.*
import com.virginiaprivacy.raydos.io.ActionType
import com.virginiaprivacy.raydos.io.StartRequest
import com.virginiaprivacy.raydos.models.TextMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.Serializable
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.system.measureTimeMillis

class SmsSender : Serializable, Service() {

    private val startTime = System.currentTimeMillis()
    var startRequest: StartRequest? = null

    private val scope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    private val textGenerator by lazy {
        Json.decodeFromString(ListSerializer(TextMessage.serializer()),
            assets.open("TextMessages.json").readBytes().decodeToString())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val channel: NotificationChannel =
        NotificationChannel(
            "1312",
            "RayDos is currently running in the background",
            NotificationManager.IMPORTANCE_DEFAULT)


    private val notification by lazy {
        NotificationCompat.Builder(this, "1312")
            .setSmallIcon(R.drawable.running_icon)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText("Running")
            .setChannelId("1312")
            .setUsesChronometer(true)
            .setWhen(startTime)
//            .setContentIntent(PendingIntent.getActivity(this, ReadyFragment::class.java))
//        .setContentIntent(PendingIntent.getActivity(baseContext, 1, Intent.makeMainActivity(
//            ComponentName.createRelative(packageName, "ReadyFragment")), PendingIntent.FLAG_CANCEL_CURRENT))
            .addAction(R.drawable.info_icon, "Info", TaskStackBuilder.create(this).run {
                addNextIntentWithParentStack(Intent(this@SmsSender, MainActivity::class.java)
                    .apply {
                        action = ActionType.RESUME_UI
                        putExtra("saved", StartRequest.from(this@SmsSender))
                    })
                    .getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT)
            })
//                Intent(createPackageContext(packageName, 0), MainActivity::class.java).apply {
//                    action = ActionType.RESUME_UI
//                }, 0, Bundle().also {
//                    it.putSerializable("restored_values", StartRequest.from(this))
//                }).)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)!!
    }


    private val running = AtomicBoolean(false)

    var logPhoneState = false

    private var messagesSent = 0

    private val smsManager by lazy {
        SmsManager.getDefault()
    }

    var useSourceField = false

    var currentSource = ""

    var currentTarget: String = ""
        set(value) {
            field = value
            scope.launch { eventBus.send(TargetNumberGeneratedEvent(value)) }
        }
    private var currentMessageText = ""
        set(value) {
            field = value
            scope.launch { eventBus.send(TextGeneratedEvent(value)) }
        }
    private var delay = 1000

    private val outbox by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            contentResolver.acquireContentProviderClient(Telephony.Sms.CONTENT_URI)
                ?.query(Telephony.Sms.CONTENT_URI, null, null, null, null)
                .let { return@lazy it }
        }
        Log.e(
            javaClass.name,
            "${Build.VERSION_CODES.O} SDK or higher is required to utilize this feature"
        )
        throw IllegalStateException()
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.action == ActionType.START_SERVICE) {
                if (it.getSerializableExtra("start_request") is StartRequest) {
                    this.startRequest = it.getSerializableExtra("start_request") as StartRequest
                    if (!running.get()) {
                        if (startRequest?.useRandomTarget!!) {
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
            }
            delay(startRequest!!.delayMillis.toLong())
        }
    }

    private suspend fun updateNotificationDescription() {
        byEventType<MessageSentEvent> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                channel.description = "RayDos has sent ${this.messageNumber} messages this session."
            }
        }
    }


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

    private fun runtime(): String {
        val runtime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Duration.of(System.currentTimeMillis(), ChronoUnit.MILLIS)
        }
        else {
            val dur = System.currentTimeMillis() - startTime
            var seconds = dur / 1000.0
            var minutes = seconds / 60
            var hours = minutes / 60
            if (hours >= 1) {
                minutes -= (60 * hours)
            }
            else {
                hours = 0.0
            }
            if (minutes >= 1) {
                seconds -= (60 * minutes)
            }
            else {
                minutes = 0.0
            }
            return "${hours}:$minutes:$seconds"
        }
        return "${runtime.toHours()}:${runtime.toMinutes()}:${runtime.seconds}"

    }

    private fun getRandomNumber(): String {
        var number = ""
        val millis = measureTimeMillis {
            while (number.length < 9) {
                number += Random.nextInt(0..9)
            }
        }
        Log.v("SmsSender", "number generated in ${millis / 1000.00} seconds")
        return number
    }

    private suspend fun sendMessage(destination: String, text: String, source: String? = null) {
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
        val eventBus: BroadcastChannel<Event> = BroadcastChannel(Channel.BUFFERED)
    }
}

suspend inline fun <reified T> byEventType(crossinline run: T.() -> Unit) {
     return SmsSender.eventBus.asFlow().filterIsInstance<T>().collect { it.run() }
}
