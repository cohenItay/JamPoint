package com.itaycohen.jampoint.data.models

import android.util.Log

sealed class QueryState {

    fun getStatusAsText() =
        when (this){
            is Failure -> "STATUS_FAILED"
            is Running -> "STATUS_RUNNING"
            is Success -> "STATUS_SUCCESS"
            is Idle -> "STATE_IDLE"
        }

    fun printStatus(logTag:String = "qwe") {
        Log.d(logTag, getStatusAsText())
    }

    object Idle: QueryState()
    object Success: QueryState()
    object Running: QueryState()
    data class Failure(val errMsg: String?): QueryState()
}