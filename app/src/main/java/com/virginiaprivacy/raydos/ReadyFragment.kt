package com.virginiaprivacy.raydos

import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import com.virginiaprivacy.raydos.events.MessageSentEvent
import com.virginiaprivacy.raydos.events.TargetNumberGeneratedEvent
import com.virginiaprivacy.raydos.settings.SettingsActivity


class ReadyFragment() : Fragment() {

    val scope = MainScope()

    private val sentReportReceiver by lazy { MessageReportReceiver() }
    private val deliveredReportReceiver by lazy { MessageReportReceiver() }
    private var running = false

    private fun getTextViewString(id: Int) =
        requireView().findViewById<TextView>(id).text.toString()

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
        outState.putSerializable(STATE, SavedRunningScreenData(
            running,
            getTextViewString(R.id.messages_sent_value).toInt(),
            getTextViewString(R.id.messages_actually_sent_value).toInt(),
            getTextViewString(R.id.messages_delivered_value).toInt(),
            0
        ))
        super.onSaveInstanceState(outState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        loadSaved(savedInstanceState)
    }

    private fun loadSaved(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            val saved: SavedRunningScreenData =
                savedInstanceState.getSerializable(STATE) as SavedRunningScreenData
            requireView().findViewById<Button>(R.id.start_button).text = with(saved.running) {
                if (this) {
                    return@with getString(R.string.stop)
                } else {
                    return@with getString(R.string.start)
                }
            }
            getTxtView(R.id.messages_sent_value).text = saved.messagesAttempted.toString()
            getTxtView(R.id.messages_actually_sent_value).text = saved.messagesSent.toString()
            getTxtView(R.id.messages_delivered_value).text = saved.messagesDelivered.toString()
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.ready_fragment, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadSaved(savedInstanceState)
        view.findViewById<Button>(R.id.button_second).setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        val targetTextValue = view.findViewById<TextView>(R.id.target_text_value)
        val messageTextValue = view.findViewById<TextView>(R.id.message_text_value)
        val messagesSentValue = view.findViewById<TextView>(R.id.messages_sent_value)
        val startButton = view.findViewById<Button>(R.id.start_button)
        val messagesActuallySent = view.findViewById<TextView>(R.id.messages_actually_sent_value)
        val messagesDelivered = view.findViewById<TextView>(R.id.messages_delivered_value)

        val startRequest = serviceIntent(ActionType.START_SERVICE)
        val stopRequest = serviceIntent(ActionType.STOP_SERVICE)

        requireActivity().registerReceiver(sentReportReceiver, IntentFilter(SmsSender.MESSAGE_SENT_INTENT))
        requireActivity().registerReceiver(deliveredReportReceiver, IntentFilter(SmsSender.MESSAGE_DELIVERED_INTENT))
        sentReportReceiver.eventsReceived.observe(viewLifecycleOwner, Observer {
            messagesActuallySent.text = it.toString()
        })
        messagesDelivered.text = deliveredReportReceiver.eventsReceived.value.toString()
        deliveredReportReceiver.eventsReceived.observe(viewLifecycleOwner, Observer {
            messagesDelivered.text = it.toString()
        })

        startButton.setOnClickListener {

            if (running) {
                running = false
                (activity as MainActivity).stopService(stopRequest)
                startButton.text = getString(R.string.start)
            }
            else {
                running = true
                startButton.text = getString(R.string.stop)

                (requireActivity() as MainActivity).startForegroundService(startRequest)
                listenForUpdates(messagesSentValue, targetTextValue)
            }
        }



    }

    private fun listenForUpdates(
        messagesSentValue: TextView,
        targetTextValue: TextView
    ) {

        val channel = SmsSender.eventBus.openSubscription()
        val iterator = channel.iterator()
        MainScope().launch {
            while (iterator.hasNext()) {
                val event = iterator.next()
                with(event) {
                    when (this::class) {
                        MessageSentEvent::class -> {
                            messagesSentValue.text =
                                (this as MessageSentEvent).messageNumber.toString()
                        }
                        TargetNumberGeneratedEvent::class -> {
                            targetTextValue.text = (this as TargetNumberGeneratedEvent).destination
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

    private fun <T> getSettingsValue(clazz: Class<T>, key: Int): T? {
        val all = PreferenceManager.getDefaultSharedPreferences(requireContext()).all
        return all.filter { it.value!!::class == clazz }[key.toString()] as T
    }

    private fun newStartRequest(view: View): StartRequest {
        val prefs = PreferenceManager.getDefaultSharedPreferences(view.context)
        val useRandomTarget =
            prefs.getBoolean(SettingsActivity.KEY_PREF_RANDOM_TARGET_SWITCH, false)
        val targetNumber = prefs
            .getString(SettingsActivity.KEY_PREF_TARGET_NUMBER_TEXT, "none")
        val useRandomMessageText =
            prefs.getBoolean(SettingsActivity.KEY_PREF_RANDOM_TEXT_SWITCH, false)
        val delay = Integer.parseInt(
            prefs.getString(
                SettingsActivity.KEY_PREF_DELAY_BETWEEN_MESSAGES,
                "1000"
            )
        )
        val defaultText = prefs.getString(SettingsActivity.KEY_PREF_DEFAULT_MESSAGE_TEXT, "")
        return StartRequest(
            useRandomTarget,
            useRandomMessageText,
            delay,
            targetNumber,
            defaultText
        )
    }

    companion object {
        const val STATE = "ready_fragment_state"
    }


}

