package com.virginiaprivacy.raydos.settings

import android.os.Bundle
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.virginiaprivacy.raydos.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import splitties.experimental.ExperimentalSplittiesApi

class SettingsFragment : PreferenceFragmentCompat() {


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    private val scope = MainScope()

    @ExperimentalSplittiesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferenceManager.findPreference<Preference>("clear_outbox")
        val target = preferenceManager.findPreference<EditTextPreference>("target_number_text")
        val useRandom =
            preferenceManager.findPreference<SwitchPreferenceCompat>("random_target_switch")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
