package com.itaycohen.jampoint.services

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentSender
import android.content.pm.ServiceInfo
import android.os.*
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.navigation.NavDeepLinkBuilder
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.utils.NotificationChannelsIds

class LocationService : Service() {

    private var serviceLooper: Looper? = null
    private val notificationManager: NotificationManagerCompat by lazy { NotificationManagerCompat.from(applicationContext) }
    private val serviceHandler: ServiceHandler by lazy {
        val handlerThread = HandlerThread("")
        handlerThread.start()
        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = handlerThread.looper
        ServiceHandler(handlerThread.looper)
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.getIntExtra(ACTION_KEY, -1)
            if (action == -1)
                return START_STICKY
            respondToControlsClick(action, startId)
        }
        serviceHandler.obtainMessage().also { msg ->
            msg.arg1 = startId
            serviceHandler.sendMessage(msg)
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        runAsForegroundService()
    }

    private fun runAsForegroundService() {
        val notification: Notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(LOCATION_SERVICE_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(LOCATION_SERVICE_NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = NavDeepLinkBuilder(applicationContext)
            .setGraph(R.navigation.nav_graph)
            .setDestination(R.id.find_jams_fragment)
            .createPendingIntent()

        return NotificationCompat.Builder(applicationContext, NotificationChannelsIds.LOCATION_CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // For Below Android 8.0
            .setSmallIcon(R.drawable.ic_baseline_my_location_24)
            .setShowWhen(false)
            .setContentTitle(applicationContext.getString(R.string.searching_jams))
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .addAction(getActionPendingIntent(ACTION_EXIT, R.drawable.ic_baseline_close_24, applicationContext.getString(R.string.halt)))
            .build()
    }

    private fun getActionPendingIntent(
        action: Int,
        @DrawableRes drawableRes: Int,
        title: String?
    ): NotificationCompat.Action {
        val intent = Intent(applicationContext, LocationService::class.java)
        intent.putExtra(ACTION_KEY, action)
        val actionPendingIntent = PendingIntent.getService(applicationContext, action, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Action(drawableRes, title, actionPendingIntent)
    }

    private fun respondToControlsClick(action: Int, startId: Int) {
        when (action) {
            ACTION_EXIT -> stopSelf(startId)
        }
    }

    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {

        }
    }

    companion object {
        private const val ACTION_KEY = "action"
        private const val ACTION_EXIT = 1
        private const val LOCATION_SERVICE_NOTIFICATION_ID = 19
    }
}