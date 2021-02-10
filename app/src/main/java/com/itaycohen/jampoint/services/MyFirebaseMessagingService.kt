package com.itaycohen.jampoint.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.itaycohen.jampoint.AppServiceLocator

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val cloudMessagingRepo = AppServiceLocator.cloudMessagingRepository

    override fun onNewToken(token: String) {
        cloudMessagingRepo.updateToken(token)
    }

    override fun onMessageReceived(p0: RemoteMessage) {

    }



    companion object {
        private val TAG = MyFirebaseMessagingService::class.simpleName
    }
}