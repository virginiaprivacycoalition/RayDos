package com.virginiaprivacy.raydos

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.M)
class PhoneStateLogger: Service() {


    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
    }


}

