package com.virginiaprivacy.raydos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import com.virginiaprivacy.raydos.events.MessageSentEvent
import com.virginiaprivacy.raydos.events.TargetNumberGeneratedEvent
import com.virginiaprivacy.raydos.settings.SettingsActivity

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ReadyFragment() : Fragment() {

    val scope = MainScope()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.ready_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        view.findViewById<Button>(R.id.button_second).setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }

        val targetTextValue = view.findViewById<TextView>(R.id.target_text_value)
        val messageTextValue = view.findViewById<TextView>(R.id.message_text_value)
        val messagesSentValue = view.findViewById<TextView>(R.id.messages_sent_value)
        val startButton = view.findViewById<Button>(R.id.start_button)

        val prefs = PreferenceManager.getDefaultSharedPreferences(view.context)
        val useRandomTarget = prefs.getBoolean(SettingsActivity.KEY_PREF_RANDOM_TARGET_SWITCH, false)
        val targetNumber = prefs
            .getString(SettingsActivity.KEY_PREF_TARGET_NUMBER_TEXT, "none")
        val useRandomMessageText = prefs.getBoolean(SettingsActivity.KEY_PREF_RANDOM_TEXT_SWITCH, false)
        val delay = Integer.parseInt(prefs.getString(SettingsActivity.KEY_PREF_DELAY_BETWEEN_MESSAGES, "1000"))
        val defaultText = prefs.getString(SettingsActivity.KEY_PREF_DEFAULT_MESSAGE_TEXT, "")
        val smsSender = SmsSender(
            this@ReadyFragment.requireContext(),
            useRandomTarget,
            useRandomMessageText,
            delay,
            targetNumber,
            defaultText
        )

        startButton.setOnClickListener {

            if (smsSender.running.get()) {
                smsSender.running.set(false)
                startButton.text = getString(R.string.start)
            }
            else {
                startButton.text = getString(R.string.stop)
                GlobalScope.launch {
                    smsSender.start()
                    (activity as)
                }
                val channel = smsSender.eventBus.openSubscription()
                val iterator = channel.iterator()
                scope.launch {
                while (iterator.hasNext()) {
                        val event = iterator.next()
                        with (event) {
                            when (this::class) {
                                MessageSentEvent::class -> {
                                    messagesSentValue.text = (this as MessageSentEvent).messageNumber.toString()
                                }
                                TargetNumberGeneratedEvent::class -> {
                                    targetTextValue.text = (this as TargetNumberGeneratedEvent).destination
                                }
                            }
                        }
                    }
                }
            }
        }

        messageTextValue.text = defaultText

        targetTextValue.text = targetNumber

    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }


}