package com.virginiaprivacy.raydos

import android.content.Intent
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.preference.PreferenceManager
import com.virginiaprivacy.raydos.settings.SettingsActivity

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        findViewById<Button>(R.id.action_settings)
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    }

    fun beginReadyActivity() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val useRandomTarget = sharedPrefs
            .getBoolean(SettingsActivity.KEY_PREF_RANDOM_TARGET_SWITCH, false)
        val useRandomText = sharedPrefs
            .getBoolean(SettingsActivity.KEY_PREF_RANDOM_TEXT_SWITCH, false)
        val delay = Integer.parseInt(sharedPrefs.getString(SettingsActivity.KEY_PREF_DELAY_BETWEEN_MESSAGES, "1000"))
        val messageTarget = when (useRandomTarget) {
            true -> null
            else -> sharedPrefs
                .getString(SettingsActivity.KEY_PREF_TARGET_NUMBER_TEXT, "")
        }
        val messageText = when (useRandomText) {
            true -> null
            else -> sharedPrefs.getString(SettingsActivity.KEY_PREF_DEFAULT_MESSAGE_TEXT, "")
        }
        messageTarget?.let { target ->
                val readyFragment = ReadyFragment()
                val bundle = Bundle()
                val smsSender = messageText?.let { text ->
                    SmsSender(
                        useRandomTarget,
                        useRandomText,
                        delay,
                        target,
                        text
                    )
                }
                bundle.putSerializable("sms_sender", smsSender)
                readyFragment.arguments = bundle

        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this,
                    classLoader.loadClass("com.virginiaprivacy.raydos.settings.SettingsActivity"))
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}