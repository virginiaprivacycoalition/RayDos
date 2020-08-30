package com.virginiaprivacy.raydos.settings

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content,
                SettingsFragment()
            )
            .commit()
    }

    companion object {
        const val KEY_PREF_RANDOM_TARGET_SWITCH = "random_target_switch"
        const val KEY_PREF_TARGET_NUMBER_TEXT = "target_number_text"
        const val KEY_PREF_RANDOM_TEXT_SWITCH = "random_text_switch"
        const val KEY_PREF_DEFAULT_MESSAGE_TEXT = "default_message_text"
        const val KEY_PREF_DELAY_BETWEEN_MESSAGES = "delay_between_messages"
        const val KEY_PREF_USE_CUSTOM_SOURCE = "sms_source"
        const val KEY_PREF_CUSTOM_SOURCE = "sms_source_manual"
        const val KEY_PREF_WRITE_LOG_FILE = "write_log_file"

    }
}