package com.virginiaprivacy.raydos

import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.preference.PreferenceManager
import com.virginiaprivacy.raydos.events.MessageSentEvent
import com.virginiaprivacy.raydos.events.TargetNumberGeneratedEvent
import com.virginiaprivacy.raydos.events.TextGeneratedEvent
import com.virginiaprivacy.raydos.io.ActionType
import com.virginiaprivacy.raydos.io.MessageReportReceiver
import com.virginiaprivacy.raydos.io.SavedRunningScreenData
import com.virginiaprivacy.raydos.io.StartRequest
import com.virginiaprivacy.raydos.services.SmsSender
import com.virginiaprivacy.raydos.services.listenByEventType
import com.virginiaprivacy.raydos.settings.SettingsActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import splitties.fragments.addToBackStack


@ExperimentalCoroutinesApi
class ReadyFragment : Fragment() {

    private val sentReportReceiver by lazy { MessageReportReceiver() }
    private val deliveredReportReceiver by lazy { MessageReportReceiver() }
    private var savedState: SavedRunningScreenData? = null
    val serviceRunning = MutableLiveData(false)
    private val messagesAttempted = MutableLiveData(0)
    private val messagesDelivered = MutableLiveData(0)
    private val messagesActuallySent = MutableLiveData(0)
    val target = MutableLiveData("")
    val messageText = MutableLiveData("")

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putSerializable(
            STATE, if (savedState != null) {
                savedState
            }
            else {
                save()
            }
        )
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ReadyFragment OnCreate", savedInstanceState.toString())
        retainInstance = true
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d("Ready", savedInstanceState.toString())
        loadSavedFromBundle(savedInstanceState)
        view?.let {
            listenForUpdates()
            addObservers(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("ReadyCreateView", savedInstanceState.toString())
        loadSavedFromBundle(savedInstanceState)
        return inflater.inflate(R.layout.ready_fragment, container, false)
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.button_second).setOnClickListener {
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                addToBackStack()
                replace(R.id.fragment_container_view,
                    InfoItemFragment())
            }
        }
        listenForUpdates()
        addObservers(view)

        loadSavedFromBundle(savedInstanceState)
        registerReceiver(sentReportReceiver, messagesAttempted)
        registerReceiver(deliveredReportReceiver, messagesDelivered)
        registerStartButtonListener(view)
    }

    private fun save(): SavedRunningScreenData {
        return SavedRunningScreenData(
            serviceRunning.value!!,
            messagesAttempted.value!!,
            messagesDelivered.value!!,
            messagesActuallySent.value!!,
            0)
    }

    private fun loadSaved(savedState: SavedRunningScreenData) {
        Log.d("ReadyFragment", savedState.toString())
        serviceRunning.value = savedState.running
        messagesAttempted.value = savedState.messagesAttempted
        messagesActuallySent.value = savedState.messagesSent
        messagesDelivered.value = savedState.messagesDelivered
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun registerStartButtonListener(view: View) {
        val startButton = view.findViewById<Button>(R.id.start_button)
        val startRequest = serviceIntent(ActionType.START_SERVICE)
        val stopRequest = serviceIntent(ActionType.STOP_SERVICE)

        startButton.setOnClickListener {
            if (serviceRunning.value!!) {
                serviceRunning.value = false
                (activity as MainActivity).stopService(stopRequest)
            }
            else {
                serviceRunning.value = true
                (requireActivity() as MainActivity).startForegroundService(startRequest)
            }
        }
    }

    private fun loadSavedFromBundle(savedInstanceState: Bundle?) {
        if (savedState == null && savedInstanceState != null) {
            savedState = savedInstanceState.getSerializable(STATE) as SavedRunningScreenData
        }
        if (savedState != null) {
            loadSaved(savedState!!)
        }
    }

    private fun registerReceiver(messageReportReceiver: MessageReportReceiver,
        fieldToUpdate: MutableLiveData<*>) {
        requireActivity().registerReceiver(
            messageReportReceiver,
            IntentFilter(SmsSender.MESSAGE_DELIVERED_INTENT))
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

    @FlowPreview
    @ExperimentalCoroutinesApi
    private fun listenForUpdates() {
        GlobalScope.launch {
            listenByEventType<MessageSentEvent> { messagesAttempted.postValue(this.messageNumber) }
        }
        GlobalScope.launch {
            listenByEventType<TargetNumberGeneratedEvent> { target.postValue(this.destination) }
        }
        GlobalScope.launch {
            listenByEventType<TextGeneratedEvent> { messageText.postValue(this.text) }
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

    private fun newStartRequest(view: View): StartRequest {
        val prefs = PreferenceManager.getDefaultSharedPreferences(view.context)
        val useRandomTarget =
            prefs.getBoolean(SettingsActivity.KEY_PREF_RANDOM_TARGET_SWITCH, false)
        val targetNumber = prefs
            .getString(SettingsActivity.KEY_PREF_TARGET_NUMBER_TEXT,
                getString(R.string.disabled_text))
        val useRandomMessageText =
            prefs.getBoolean(SettingsActivity.KEY_PREF_RANDOM_TEXT_SWITCH, false)
        val delay = Integer.parseInt(
            prefs.getString(
                SettingsActivity.KEY_PREF_DELAY_BETWEEN_MESSAGES,
                "3000"
            ).toString()
        )
        val useSmsSource = prefs.getBoolean(SettingsActivity.KEY_PREF_USE_CUSTOM_SOURCE, false)
        val smsCustomSource = prefs.getString(SettingsActivity.KEY_PREF_CUSTOM_SOURCE,
            getString(R.string.disabled_text))
        val defaultText = prefs.getString(SettingsActivity.KEY_PREF_DEFAULT_MESSAGE_TEXT, "")
        val logPhoneState = prefs.getBoolean(SettingsActivity.KEY_PREF_WRITE_LOG_FILE, false)
        return StartRequest(
            useRandomTarget,
            useRandomMessageText,
            delay,
            targetNumber,
            defaultText,
            useSmsSource,
            smsCustomSource,
            logPhoneState
        )
    }

    companion object {
        const val STATE = "ready_fragment_state"
    }
}



