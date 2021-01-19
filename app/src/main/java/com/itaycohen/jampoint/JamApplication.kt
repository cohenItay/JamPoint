package com.itaycohen.jampoint

import android.app.Application
import com.google.android.libraries.places.api.Places
import com.itaycohen.jampoint.data.AppServiceLocator

class JamApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        AppServiceLocator.getInstance(applicationContext)
    }
}