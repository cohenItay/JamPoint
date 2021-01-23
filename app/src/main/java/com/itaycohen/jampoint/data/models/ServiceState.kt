package com.itaycohen.jampoint.data.models

import android.util.Log

sealed class ServiceState {

    fun getStatusAsText() =
        when (this){
            is Idle -> "Idle"
            is Available -> "Running"
            is Unavailable -> "Disabled"
        }

    fun printStatus(logTag:String = "qwe") {
        Log.d(logTag, getStatusAsText())
    }

    object Idle: ServiceState()
    object Available: ServiceState()
    data class Unavailable(val errMsg: String? = null): ServiceState()
}