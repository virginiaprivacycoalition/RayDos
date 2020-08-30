package com.virginiaprivacy.raydos.settings

import android.Manifest
import android.content.ContentProviderClient
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.virginiaprivacy.raydos.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import splitties.experimental.ExperimentalSplittiesApi
import splitties.permissions.PermissionRequestResult
import splitties.permissions.hasPermission
import splitties.permissions.requestPermission

class SettingsFragment : PreferenceFragmentCompat() {


    private fun outbox(outbox: ContentProviderClient.() -> Unit) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val client =
                    context?.contentResolver?.acquireContentProviderClient(Telephony.Sms.CONTENT_URI)
                        ?.apply {
                            this.outbox()
                            this.close()
                        }
                client?.release()
            } else {
                Log.e(javaClass.name,
                    "${Build.VERSION_CODES.O} SDK or higher is required to utilize this feature")
                throw IllegalStateException()
            }
        }


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

    }

    private val scope = MainScope()

    @ExperimentalSplittiesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clearOutboxButton = preferenceManager.findPreference<Preference>("clear_outbox")
        clearOutboxButton?.let { preference ->
            if (hasReadSMSPermission()) {
                outbox {
                    this.query(Telephony.Sms.Outbox.CONTENT_URI, null, null, null, null)
                        ?.let { cursor ->
                            preference.summary =
                                "There are currently ${cursor.columnCount} unsent messages in the outbox."
                            cursor.close()
                        }
                }
            }
                preference.setOnPreferenceClickListener {
                    if (!hasReadSMSPermission()) {
                        scope.launch {
                            if (requestPermission(childFragmentManager, lifecycle, Manifest.permission.READ_SMS)
                                        is PermissionRequestResult.Denied) {
                                // Do nothing
                            }
                            else {
                                deleteOutboxMessages(preference)
                            }
                        }
                        false
                    }
                    else {
                        deleteOutboxMessages(preference)
                        true
                    }
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private fun deleteOutboxMessages(preference: Preference) {
        outbox {
            delete(Telephony.Sms.Outbox.CONTENT_URI, null, null)
        }
        preference.summary = "there are currently 0 unsent messages in the outbox."
        Log.i("Settings", "Cleared outbox SMS messages.")
    }

    @ExperimentalSplittiesApi
    private fun hasReadSMSPermission() = hasPermission(Manifest.permission.READ_SMS)


}
