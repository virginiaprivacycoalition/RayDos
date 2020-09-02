package com.virginiaprivacy.raydos

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.virginiaprivacy.raydos.infoitem.InfoItem
import kotlinx.coroutines.ExperimentalCoroutinesApi

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

    private val firstInfoItem = InfoItem(getString(R.string.info_item_1_title),
        listOf(getString(R.string.info_item_details_1),
            getString(R.string.info_item_details_2),
            getString(R.string.info_item_details_3),
            getString(R.string.info_item_details_4)))

    private val secondInfoItem = InfoItem(getString(R.string.info_item_2_title),
        listOf(getString(R.string.info_item_details_5),
            getString(R.string.info_item_details_6),
            getString(R.string.info_item_details_7),
            getString(R.string.info_item_details_8)))

    private val thirdInfoItem = InfoItem(getString(R.string.info_item_3_title),
        listOf(getString(R.string.info_item_details_9),
            getString(R.string.info_item_details_10),
            getString(R.string.info_item_details_11)))

    private val fourthInfoItem = InfoItem(getString(R.string.info_item_4_title),
        listOf(getString(R.string.info_item_details_12)))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.info_fragment_item_list, container, false)

        val infoItems = ArrayList<InfoItem>()
        infoItems += firstInfoItem
        infoItems += secondInfoItem
        infoItems += thirdInfoItem
        infoItems += fourthInfoItem
        val rv = view.findViewById<RecyclerView>(R.id.info_item_rv)
        rv.layoutManager = LinearLayoutManager(view.context)
        rv.adapter = InfoItemAdapter(infoItems, requireContext())

        rv.setHasFixedSize(true)
        rv.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        disableButton(nextButton(view))

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
            nextButton(it).run {
                this?.isClickable = true
                this?.isEnabled = true
            }
        }
    }

    private fun disableButton(next: Button?) {
        context?.let {
            if (!hasSendPermission()) {
                next?.isEnabled = false
                next?.isClickable = false
                requestSmsPermission()
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

    private fun nextButton(view: View): Button? = view.findViewById(R.id.info_item_button)

    companion object {

        // TODO: Customize parameter argument names
        const val ARG_COLUMN_COUNT = "column-count"

    }
}