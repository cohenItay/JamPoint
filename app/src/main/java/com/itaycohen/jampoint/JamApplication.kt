package com.itaycohen.jampoint

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.android.libraries.places.api.Places
import com.itaycohen.jampoint.utils.NotificationsUtil

class JamApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        AppServiceLocator.initWith(applicationContext)
        AppServiceLocator.cloudMessagingRepository.updateToken()
        Places.initialize(applicationContext, BuildConfig.PLACES_API_KEY)
        NotificationsUtil.createNotificationChannel(applicationContext)
    }
}