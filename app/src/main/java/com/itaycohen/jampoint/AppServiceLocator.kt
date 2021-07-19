package com.itaycohen.jampoint

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.itaycohen.jampoint.data.repositories.CloudMessagingRepository
import com.itaycohen.jampoint.data.repositories.LocationRepository
import com.itaycohen.jampoint.data.repositories.JamPlacesRepository
import com.itaycohen.jampoint.data.repositories.UserRepository
import com.itaycohen.jampoint.utils.GsonContainer
import com.itaycohen.jampoint.utils.SharedPrefsHelper

object AppServiceLocator {

    private lateinit var appContext: Context

    fun initWith(appContext: Context) {
        this.appContext = appContext
    }

    val userRepository: UserRepository by lazy { UserRepository(
        appContext,
        Firebase.database,
        SharedPrefsHelper.Factory(appContext, GsonContainer.instance).create("User_prefs")
    ) }
    val cloudMessagingRepository: CloudMessagingRepository by lazy { CloudMessagingRepository(appContext, Firebase.database) }
    val locationRepository: LocationRepository by lazy {
        LocationRepository(
            appContext,
            LocationServices.getFusedLocationProviderClient(appContext)
        )
    }
    val jamPlacesRepository: JamPlacesRepository by lazy { JamPlacesRepository(appContext, Firebase.database) }
}