package com.itaycohen.jampoint

import android.app.Application
import com.google.android.libraries.places.api.Places

class JamApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Places.initialize(applicationContext, MAPS_API_KEY);
    }

    companion object {
        private const val MAPS_API_KEY = "AIzaSyAbZfPJI61Qk7bgt7CqpRtuc7AG5vw08qM"
    }
}