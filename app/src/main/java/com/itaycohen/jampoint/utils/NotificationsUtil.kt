package com.itaycohen.jampoint.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.itaycohen.jampoint.R

object NotificationsUtil {

    fun createNotificationChannel(appContext: Context) = with(appContext) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var name = getString(R.string.location_updates)
            var descriptionText = getString(R.string.location_messages_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val locationChannel = NotificationChannel(
                getString(R.string.location_notification_channel_id),
                name,
                importance
            ).apply {
                description = descriptionText
            }

            name = getString(R.string.location_updates)
            descriptionText = getString(R.string.social_messages_description)
            val socialChannel = NotificationChannel(
                getString(R.string.social_notification_channel_id),
                name,
                importance
            ).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(locationChannel)
            notificationManager.createNotificationChannel(socialChannel)
        }
    }
}