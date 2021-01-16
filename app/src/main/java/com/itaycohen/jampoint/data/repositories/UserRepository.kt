package com.itaycohen.jampoint.data.repositories

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.itaycohen.jampoint.data.UserResult

class UserRepository(
    private val appContext: Context
) {

    val userLiveData: LiveData<FirebaseUser?> =  MutableLiveData(null)

    init {
        (userLiveData as MutableLiveData).value = FirebaseAuth.getInstance().currentUser
    }

    fun createFirebaseLoginContract() = object : ActivityResultContract<Unit?, UserResult>() {

        override fun createIntent(context: Context, input: Unit?): Intent {
            val providers = arrayListOf(
                AuthUI.IdpConfig.EmailBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build(),
                AuthUI.IdpConfig.FacebookBuilder().build()
            )
            return AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
        }

        override fun parseResult(resultCode: Int, intent: Intent?): UserResult {
            intent ?: return UserResult(null, null) // don't do nothing
            val response = IdpResponse.fromResultIntent(intent)
            val userResult =  if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                UserResult(FirebaseAuth.getInstance().currentUser, null)
            } else {
                // Sign in failed. If response is null the user canceled the sign-in flow using the back button.
                UserResult(null, response?.error)
            }
            (userLiveData as MutableLiveData).value = userResult.user
            return userResult
        }
    }

    fun doLogout() {
        AuthUI.getInstance()
            .signOut(appContext)
            .addOnCompleteListener {
                (userLiveData as MutableLiveData).value = null
            }
    }
}