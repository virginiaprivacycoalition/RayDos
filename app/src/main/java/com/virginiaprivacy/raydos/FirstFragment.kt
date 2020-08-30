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
import androidx.fragment.app.commit

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val next = nextButton(view)
        disableButton(next)
        next?.setOnClickListener {
            parentFragmentManager.commit {
                replace(R.id.fragment_container_view, parentFragmentManager.findFragmentById(R.id.SecondFragment) ?: ReadyFragment())
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

    private fun nextButton(view: View): Button? = view.findViewById(R.id.button_first)

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
}