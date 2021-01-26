package com.itaycohen.jampoint

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.android.libraries.places.api.Places
import com.itaycohen.jampoint.utils.NotificationChannelsIds

class JamApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        AppServiceLocator.initWith(applicationContext)
        Places.initialize(applicationContext, BuildConfig.PLACES_API_KEY)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.desc_play)
            val descriptionText = getString(R.string.audio_player_secription)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NotificationChannelsIds.LOCATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}