package com.itaycohen.jampoint.data.repositories

import android.content.Context
import android.location.Location
import androidx.lifecycle.*
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.itaycohen.jampoint.data.models.ServiceState

class LocationRepository(
    private val appContext: Context
) {

    val locationStateLiveData: LiveData<ServiceState> = MutableLiveData(ServiceState.Idle)
    val locationLiveData: LiveData<Location?> = MutableLiveData(null)
    var locationServiceLifeCycle: Lifecycle? = null
        set(value) {
            field?.also { it.removeObserver(serviceLifeCycleObserver) }
            field = value
            value?.also { it.addObserver(serviceLifeCycleObserver) }
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

    private val serviceLifeCycleObserver = object : LifecycleObserver {

        private val serviceState = this@LocationRepository.locationStateLiveData as MutableLiveData

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        fun onOwnerCreated() {
            serviceState.postValue(ServiceState.Idle)
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onOwnerSDestroyed() {
            serviceState.postValue(ServiceState.Idle)
        }
    }
}