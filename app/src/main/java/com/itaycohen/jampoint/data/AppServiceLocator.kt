package com.itaycohen.jampoint.data

import android.content.Context
import com.itaycohen.jampoint.data.repositories.LocationRepository
import com.itaycohen.jampoint.data.repositories.UserRepository

class AppServiceLocator private constructor(private val appContext: Context) {

    val userRepository = UserRepository(appContext)
    val locationRepository = LocationRepository(appContext)

    companion object {
        lateinit var instance: AppServiceLocator
            private set
        fun getInstance(appContext: Context): AppServiceLocator {
            instance = AppServiceLocator((appContext))
            return instance
        }
    }
}