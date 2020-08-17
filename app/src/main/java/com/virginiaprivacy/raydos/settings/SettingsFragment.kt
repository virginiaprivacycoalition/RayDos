package com.virginiaprivacy.raydos.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.raydos.raydos.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }
}