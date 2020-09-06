package com.virginiaprivacy.raydos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.preference.PreferenceManager
import com.cioccarellia.ksprefs.KsPrefs
import com.virginiaprivacy.raydos.io.ActionType
import com.virginiaprivacy.raydos.io.StartRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import splitties.alertdialog.appcompat.alertDialog

@ExperimentalCoroutinesApi
class MainActivity : AppCompatActivity() {

    @ExperimentalCoroutinesApi
    var readyFragment: ReadyFragment? = null
    get() {
        if (field == null) {
            field = ReadyFragment()
        }
        return field
    }

    private val infoItemFragment: InfoItemFragment by lazy {
        InfoItemFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = this
        if (intent.action == ActionType.RESUME_UI) {
            Log.d("MainActivity", "App resuming from notification bar")
            setContentView(R.layout.activity_main)

            supportFragmentManager.commit {
                this.replace(R.id.fragment_container_view,
                    readyFragment!!.apply {
                        serviceRunning.postValue(true)
                        val startRequest = intent.getSerializableExtra("saved") as StartRequest
                        messageText.postValue(startRequest.nonRandomText)
                        target.postValue(startRequest.target)
                    })
            }

            if (savedInstanceState != null) {
                Log.d("MainActivity", "Saved instance exists $savedInstanceState")
                return
            }
            setSupportActionBar(findViewById(R.id.toolbar))
            findViewById<Button>(R.id.action_settings)
        }
        else {
            setContentView(R.layout.activity_main)
            if (!prefs.pull("approved_guidelines", false)) {
                alertDialog {
                    this.setTitle(getString(R.string.usage_guidelines_title))
                    setMessage(getString(R.string.usage_guidelines_message))
                    setPositiveButton(getString(R.string.usage_guidelines_positive_button)) { dialog, _ ->
                        dialog.dismiss()
                        prefs.push("approved_guidelines", true)
                    }
                    setNegativeButton(getString(R.string.usage_guidelines_negative_button)) { dialog, _ ->
                        dialog.dismiss()
                        finish()
                    }
                }.show()
            }

            supportFragmentManager.commit {
                replace(R.id.fragment_container_view, infoItemFragment)
            }

            setSupportActionBar(findViewById(R.id.toolbar))
            findViewById<Button>(R.id.action_settings)
        }

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
    }

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        Log.d("ActivityPersistent",
            "saved: ${savedInstanceState.toString()} persistent: ${persistentState.toString()}")
        super.onCreate(savedInstanceState, persistentState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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

    companion object {
        lateinit var appContext: Context
        val prefs by lazy { KsPrefs(appContext) }
    }
}
