package com.itaycohen.jampoint.data.repositories

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

class CloudMessagingRepository(
    private val appContext: Context,
    private val database: FirebaseDatabase
) {

    fun updateToken(updatedToken: String? = null) {
        if (updatedToken == null) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                val token = task.result
                Log.d(TAG, "updateToken: $token")
            })
        } else {
            Log.d(TAG, "updateToken: $updatedToken")
        }
    }

    companion object {
        private val TAG = CloudMessagingRepository::class.simpleName
    }
}