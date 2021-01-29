package com.itaycohen.jampoint.data.repositories

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.google.android.gms.location.*
import com.itaycohen.jampoint.data.models.ServiceState
import com.itaycohen.jampoint.services.LocationService

class LocationRepository(
    private val appContext: Context,
    private val fusedLocationClient: FusedLocationProviderClient
) {

    private var serviceStateLiveDataSwitcher: MutableLiveData<LiveData<ServiceState>> =
        MutableLiveData(MutableLiveData(ServiceState.Idle))
    private var locationLiveDataSwitcher: MutableLiveData<LiveData<Location?>> =
        MutableLiveData(MutableLiveData(null))
    private val repositoryLocationLiveData: LiveData<Location?> = MutableLiveData(null)

    @Suppress("UNCHECKED_CAST")
    val serviceStateLiveData: LiveData<ServiceState> = serviceStateLiveDataSwitcher.switchMap { it }
    val locationLiveData: LiveData<Location?> = locationLiveDataSwitcher.switchMap { it }

    init {
        switchToLiveDatas(repositoryLocationLiveData)
        val permissionState = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                (repositoryLocationLiveData as MutableLiveData).postValue(it)
            }
        }
    }

    fun runLocationRequest(locationReq: LocationRequest) {
        if (locationReq.numUpdates > 3)
            startLocationService(locationReq)
        else {
            runSingleLocationUpdate(locationReq)
        }
    }

    fun switchToLiveDatas(
        locationLiveDataSource: LiveData<Location?>,
        stateLiveDataSource: LiveData<ServiceState> = MutableLiveData(ServiceState.Idle)
    ) {
        locationLiveDataSwitcher.postValue(locationLiveDataSource)
        serviceStateLiveDataSwitcher.postValue(stateLiveDataSource)
    }

    fun stopLocationService() {
        val state = serviceStateLiveData.value
        if (state == null || state == ServiceState.Idle) return

        val intent = Intent(appContext, LocationService::class.java)
        intent.putExtra(LocationService.ACTION_KEY, LocationService.ACTION_EXIT)
        appContext.startService(intent)
    }






    private val singleLocationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult?) {
            val locationResult = p0 ?: return
            // Here i can decide which location to use, based on each location parmas accuracy
            // I'll just use the latest.
            locationLiveData as MutableLiveData
            locationLiveData.postValue(locationResult.lastLocation)
            fusedLocationClient.removeLocationUpdates(this)
        }
    }

    private fun startLocationService(locationRequest: LocationRequest) {
        check(locationRequest.interval > 0)
        val intent = Intent(appContext, LocationService::class.java).apply {
            putExtra(LocationService.LOCATION_REQUEST_PARAMS, locationRequest)
        }
        ContextCompat.startForegroundService(appContext, intent)
    }

    private fun runSingleLocationUpdate(locationReq: LocationRequest) {
        val permissionState = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            switchToLiveDatas(repositoryLocationLiveData)
            fusedLocationClient.removeLocationUpdates(singleLocationCallback)
            fusedLocationClient.requestLocationUpdates(
                locationReq,
                singleLocationCallback,
                Looper.myLooper()
            )
        }
    }
}