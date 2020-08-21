package com.virginiaprivacy.raydos

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import com.virginiaprivacy.raydos.events.MessageSentEvent
import com.virginiaprivacy.raydos.events.TargetNumberGeneratedEvent
import com.virginiaprivacy.raydos.events.TextGeneratedEvent
import com.virginiaprivacy.raydos.io.ActionType
import com.virginiaprivacy.raydos.io.MessageReportReceiver
import com.virginiaprivacy.raydos.io.SavedRunningScreenData
import com.virginiaprivacy.raydos.io.StartRequest
import com.virginiaprivacy.raydos.services.SmsSender
import com.virginiaprivacy.raydos.settings.SettingsActivity


class ReadyFragment() : Fragment() {

    val scope = MainScope()

    private val sentReportReceiver by lazy { MessageReportReceiver() }
    private val deliveredReportReceiver by lazy { MessageReportReceiver() }
    private var savedState: SavedRunningScreenData? = null
    private val serviceRunning = MutableLiveData(false)
    private val messagesAttempted = MutableLiveData(0)
    private val messagesDelivered = MutableLiveData(0)
    private val messagesActuallySent = MutableLiveData(0)
    private val target = MutableLiveData("")
    private val messageText = MutableLiveData("")

    private fun getTxtView(id: Int) = requireView().findViewById<TextView>(id)


//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        savedInstanceState?.let {
//            val saved: SavedRunningScreenData = it.get(STATE) as SavedRunningScreenData
//            running = true
//            getTxtView(R.id.messages_sent_value).text = saved.messagesAttempted.toString()
//        }
//    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(
            STATE, if (savedState != null) {
                savedState
            } else {
                save()
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true;
    }

    override fun onActivityCreated(savedInstanceState: Bundle?)
    {
        super.onActivityCreated(savedInstanceState)
        loadSavedFromBundle(savedInstanceState)
        view?.let {
            listenForUpdates()
            addObservers(it)
        }

    }

    private fun save(): SavedRunningScreenData
    {
        return SavedRunningScreenData(
            serviceRunning.value!!,
            messagesAttempted.value!!,
            messagesDelivered.value!!,
            messagesActuallySent.value!!,
            0
        )
    }

    private fun loadSaved(savedState: SavedRunningScreenData) {
        Log.d("ReadyFragment", savedState.toString())
        serviceRunning.value = savedState.running
        messagesAttempted.value = savedState.messagesAttempted
        messagesActuallySent.value = savedState.messagesSent
        messagesDelivered.value = savedState.messagesDelivered
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        loadSavedFromBundle(savedInstanceState)
        return inflater.inflate(R.layout.ready_fragment, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.button_second).setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
        listenForUpdates()
        addObservers(view)

        loadSavedFromBundle(savedInstanceState)


        registerReceiver(sentReportReceiver, messagesAttempted)
        registerReceiver(deliveredReportReceiver, messagesDelivered)

        registerStartButtonListener(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun registerStartButtonListener(view: View)
    {
        val startButton = view.findViewById<Button>(R.id.start_button)

        val startRequest = serviceIntent(ActionType.START_SERVICE)
        val stopRequest = serviceIntent(ActionType.STOP_SERVICE)

        startButton.setOnClickListener {
            if (serviceRunning.value!!)
            {
                serviceRunning.value = false
                (activity as MainActivity).stopService(stopRequest)
            }
            else
            {
                serviceRunning.value = true
                (requireActivity() as MainActivity).startForegroundService(startRequest)
            }
        }
    }

    private fun loadSavedFromBundle(savedInstanceState: Bundle?)
    {
        fun loadSaved(savedState: SavedRunningScreenData) {
            Log.d("ReadyFragment", savedState.toString())
            serviceRunning.value = savedState.running
            messagesAttempted.value = savedState.messagesAttempted
            messagesActuallySent.value = savedState.messagesSent
            messagesDelivered.value = savedState.messagesDelivered
        }
        if (savedState == null && savedInstanceState != null)
        {
            savedState = savedInstanceState.getSerializable(STATE) as SavedRunningScreenData
        }
        if (savedState != null)
        {
            loadSaved(savedState!!)
        }
    }

    private fun registerReceiver(messageReportReceiver: MessageReportReceiver, fieldToUpdate: MutableLiveData<*>)
    {
        requireActivity().registerReceiver(
            messageReportReceiver,
            IntentFilter(SmsSender.MESSAGE_DELIVERED_INTENT)
        )
        messageReportReceiver.eventsReceived.observe(viewLifecycleOwner, Observer {
            fieldToUpdate.value = it
        })
    }

    private fun addObservers(v: View) {
        messagesAttempted.observe(viewLifecycleOwner) {
            v.findViewById<TextView>(R.id.messages_sent_value).text =
                it.toString()
        }
        messagesDelivered.observe(viewLifecycleOwner) {
            v.findViewById<TextView>(R.id.messages_delivered_value).text = it.toString()
        }
        messagesActuallySent.observe(viewLifecycleOwner) {
            v.findViewById<TextView>(R.id.messages_actually_sent_value).text = it.toString()
        }
        serviceRunning.observe(viewLifecycleOwner) {
            v.findViewById<Button>(R.id.start_button).text = when (it) {
                true -> getString(R.string.stop)
                false -> getString(R.string.start)
            }
        }
        target.observe(viewLifecycleOwner) {
            v.findViewById<TextView>(R.id.target_text_value).text = it
        }
        messageText.observe(viewLifecycleOwner) {
            v.findViewById<TextView>(R.id.message_text_value).run {
                this.text = it
            }
        }
    }

    private fun listenForUpdates() {
        val channel = SmsSender.eventBus.openSubscription()
        val iterator = channel.iterator()
        GlobalScope.launch {
            while (iterator.hasNext()) {
                val event = iterator.next()
                with(event) {
                    when (this::class) {
                        MessageSentEvent::class -> {
                            messagesAttempted.postValue(
                                (this as MessageSentEvent).messageNumber)
                        }
                        TargetNumberGeneratedEvent::class -> {
                            target.postValue((this as TargetNumberGeneratedEvent).destination)
                        }
                        TextGeneratedEvent::class -> {
                            messageText.postValue((this as TextGeneratedEvent).text)
                        }
                    }
                }
            }
        }
    }

    private fun serviceIntent(actionType: String): Intent {
        if (actionType != ActionType.START_SERVICE && actionType != ActionType.STOP_SERVICE) {
            throw (IllegalArgumentException())
        }
        val startRequest = newStartRequest(requireView())
        val intent = Intent(requireActivity(), SmsSender::class.java)
        intent.action = actionType
        intent.putExtra("start_request", startRequest)
        return intent
    }

    private fun newStartRequest(view: View): StartRequest
    {
        val prefs = PreferenceManager.getDefaultSharedPreferences(view.context)
        val useRandomTarget =
            prefs.getBoolean(SettingsActivity.KEY_PREF_RANDOM_TARGET_SWITCH, false)
        val targetNumber = prefs
            .getString(SettingsActivity.KEY_PREF_TARGET_NUMBER_TEXT, getString(R.string.disabled_text))
        val useRandomMessageText =
            prefs.getBoolean(SettingsActivity.KEY_PREF_RANDOM_TEXT_SWITCH, false)
        val delay = Integer.parseInt(
            prefs.getString(
                SettingsActivity.KEY_PREF_DELAY_BETWEEN_MESSAGES,
                "1000"
            ).toString()
        )
        val useSmsSource = prefs.getBoolean(SettingsActivity.KEY_PREF_USE_CUSTOM_SOURCE, false)
        val smsCustomSource = prefs.getString(SettingsActivity.KEY_PREF_CUSTOM_SOURCE, getString(R.string.disabled_text))
        val defaultText = prefs.getString(SettingsActivity.KEY_PREF_DEFAULT_MESSAGE_TEXT, "")
        return StartRequest(
            useRandomTarget,
            useRandomMessageText,
            delay,
            targetNumber,
            defaultText,
            useSmsSource,
            smsCustomSource
        )
    }

    companion object {
        const val STATE = "ready_fragment_state"
    }
}

fun <T> nn(first: T?, second: T?): T {
    return when (first == null) {
        true -> second!!
        else -> first
    }
}


