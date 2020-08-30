package com.virginiaprivacy.raydos

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.M)
class PhoneStateLogger: Service() {


    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }


}

