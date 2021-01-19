package com.itaycohen.jampoint.services

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MyLocationService : Service() {

    override fun onBind(intent: Intent): IBinder? = null
}