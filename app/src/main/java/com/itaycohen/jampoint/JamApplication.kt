package com.itaycohen.jampoint

import android.app.Application
import com.google.android.libraries.places.api.Places

class JamApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
    }
}