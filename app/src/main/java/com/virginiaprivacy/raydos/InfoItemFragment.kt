package com.virginiaprivacy.raydos

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.virginiaprivacy.raydos.infoitem.InfoItem
import com.virginiaprivacy.raydos.settings.SettingsActivity
import jp.wasabeef.recyclerview.animators.LandingAnimator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.tinkoff.scrollingpagerindicator.ScrollingPagerIndicator
import splitties.views.onClick


/**
 * A fragment representing a list of Items.
 */
@ExperimentalCoroutinesApi
class InfoItemFragment : Fragment() {

    private var columnCount = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    private val firstInfoItem by lazy {
        InfoItem(getString(R.string.info_item_1_title),
            listOf(getString(R.string.info_item_details_1),
                getString(R.string.info_item_details_2),
                getString(R.string.info_item_details_3),
                getString(R.string.info_item_details_4)))
    }

    private val secondInfoItem by lazy {
        InfoItem(getString(R.string.info_item_2_title),
            listOf(getString(R.string.info_item_details_5),
                getString(R.string.info_item_details_6),
                getString(R.string.info_item_details_7),
                getString(R.string.info_item_details_8)))
    }

    private val thirdInfoItem by lazy {
        InfoItem(getString(R.string.info_item_3_title),
            listOf(getString(R.string.info_item_details_9),
                getString(R.string.info_item_details_10),
                getString(R.string.info_item_details_11)))
    }

    private val fourthInfoItem by lazy {
        InfoItem(getString(R.string.info_item_4_title),
            listOf(getString(R.string.info_item_details_12)))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.info_fragment_item_list, container, false)

        val infoItems = ArrayList<InfoItem>()
        infoItems += firstInfoItem
        infoItems += secondInfoItem
        infoItems += thirdInfoItem
        infoItems += fourthInfoItem
        val rv = view.findViewById<RecyclerView>(R.id.info_item_rv)
        rv.layoutManager = LinearLayoutManager(view.context, LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = InfoItemAdapter(infoItems)
        rv.setHasFixedSize(true)
        LinearSnapHelper().attachToRecyclerView(rv)
        rv.itemAnimator = LandingAnimator()
        //rv.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        val recyclerIndicator: ScrollingPagerIndicator =
            view.findViewById(R.id.indicator)
        recyclerIndicator.attachToRecyclerView(rv)
        val useRandomTarget = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(SettingsActivity.KEY_PREF_RANDOM_TARGET_SWITCH, false)
        val targetText = MainActivity.prefs.pull(SettingsActivity.KEY_PREF_TARGET_NUMBER_TEXT, "")
        val continueButton = view.findViewById<Button>(R.id.continue_button)
        val configureButton = view.findViewById<Button>(R.id.configure_button)
        val errorText = view.findViewById<TextView>(R.id.info_error_message)

        if (!useRandomTarget && targetText.length <= 1) {
            errorText.text = getString(R.string.destination_number_error_message)
            errorText.visibility = TextView.VISIBLE
            continueButton.isEnabled = false
        }
        configureButton.onClick {
            val intent = Intent(context, SettingsActivity::class.java)
            startActivity(intent)
        }
        continueButton.onClick {
            parentFragmentManager.commit {
                replace(R.id.fragment_container_view, (activity as MainActivity).readyFragment!!)
            }
        }

        return view
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        println(grantResults.asList())
        if (!permissions.contentEquals(arrayOf(Manifest.permission.SEND_SMS))) {
            return
        }
        view?.let {
            if (grantResults[0] == -1) {
                AlertDialog.Builder(context).run {
                    setMessage(getString(R.string.sms_permission_dialog_message))
                    setPositiveButton(getString(R.string.sms_permission_dialog_retry_button)) { dialog, _ ->
                        dialog.dismiss()
                        requestSmsPermission()
                    }
                    setNegativeButton(getString(R.string.sms_permission_dialog_exit_button)) { dialogInterface, _ ->
                        dialogInterface.cancel()
                        (activity as MainActivity).finish()
                    }
                    show()
                }
                return@onRequestPermissionsResult
            }
        }
    }

    private fun requestSmsPermission() =
        requestPermissions(arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS), 1)

    private fun hasSendPermission(): Boolean =
        context.let {
            it?.let { it1 ->
                ActivityCompat.checkSelfPermission(it1, Manifest.permission.SEND_SMS)
                    .and(ActivityCompat.checkSelfPermission(it1, Manifest.permission.READ_SMS))
            } == PackageManager.PERMISSION_GRANTED
        }

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

    }
}