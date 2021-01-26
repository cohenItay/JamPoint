package com.itaycohen.jampoint.services

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.*
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavDeepLinkBuilder
import com.google.android.gms.location.*
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.ServiceState
import com.itaycohen.jampoint.data.repositories.LocationRepository
import com.itaycohen.jampoint.utils.NotificationChannelsIds

class LocationService : LifecycleService() {

    private var serviceLooper: Looper? = null
    private lateinit var locationRequest: LocationRequest
    private val locationStateLiveData: LiveData<ServiceState> = MutableLiveData(ServiceState.Idle)
    private val locationLiveData: LiveData<Location?> = MutableLiveData(null)
    private val fusedLocationClient: FusedLocationProviderClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val locationRepository: LocationRepository = AppServiceLocator.locationRepository
    private val serviceHandler: ServiceHandler by lazy {
        val handlerThread = HandlerThread("")
        handlerThread.start()
        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = handlerThread.looper
        ServiceHandler(handlerThread.looper)
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        locationRepository.switchToLiveDatas(
            locationLiveData,
            locationStateLiveData
        )
        (locationStateLiveData as MutableLiveData).postValue(ServiceState.Idle)
        runAsForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        intent ?: return START_STICKY

        val action = intent.getIntExtra(ACTION_KEY, -1)
        if (action != -1) {
            respondToControlsClick(action, startId)
            return START_STICKY
        }

        intent.getParcelableExtra<LocationRequest>(LOCATION_REQUEST_PARAMS)?.also { locationReq ->
            if (::locationRequest.isInitialized)
                if (locationRequest == locationReq)
                    return@also

            this.locationRequest = locationReq
            serviceHandler.obtainMessage().also { msg ->
                msg.arg1 = startId
                msg.obj = locationReq
                serviceHandler.sendMessage(msg)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        (locationStateLiveData as MutableLiveData).postValue(ServiceState.Idle)
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
            ACTION_EXIT -> {
                fusedLocationClient.removeLocationUpdates(locationUpdatesCallback)
                stopSelf(startId)
            }
        }
    }

    private inner class ServiceHandler(looper: Looper) : Handler(looper) {

        override fun handleMessage(msg: Message) {
            (msg.obj as? LocationRequest)?.also { locationReq ->
                val permissionState = ContextCompat.checkSelfPermission(this@LocationService, Manifest.permission.ACCESS_FINE_LOCATION)
                if (permissionState == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.removeLocationUpdates(locationUpdatesCallback)
                    fusedLocationClient.requestLocationUpdates(
                        locationReq,
                        locationUpdatesCallback,
                        Looper.myLooper()
                    )
                }
            }
        }
    }

    val locationUpdatesCallback = object : LocationCallback() {

        override fun onLocationAvailability(p0: LocationAvailability?) {
            val availability = p0 ?: return
            locationStateLiveData as MutableLiveData
            locationStateLiveData.postValue(if (availability.isLocationAvailable)
                ServiceState.Available
            else
                ServiceState.Unavailable()
            )
        }

        override fun onLocationResult(p0: LocationResult?) {
            val locationResult = p0 ?: return
            // Here i can decide which location to use, based on each location parmas accuracy
            // I'll just use the latest.
            locationLiveData as MutableLiveData
            locationLiveData.postValue(locationResult.lastLocation)
        }
    }

    companion object {
        const val ACTION_KEY = "action"
        const val ACTION_EXIT = 1
        private const val LOCATION_SERVICE_NOTIFICATION_ID = 19
        const val LOCATION_REQUEST_PARAMS = "Locare342xxparam0"
    }
}