package com.itaycohen.jampoint.data.repositories

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.itaycohen.jampoint.data.UserResult
import com.itaycohen.jampoint.data.models.User
import com.itaycohen.jampoint.utils.highQualityPhotoUri

class UserRepository(
    private val appContext: Context,
    private val database: FirebaseDatabase
) {

    private val firebaseUserLiveData: LiveData<FirebaseUser?> = MutableLiveData(null)

    val userLiveData: LiveData<User?> =  firebaseUserLiveData.map { firebaseUser ->
        toUser(firebaseUser)
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener { firebaseUser ->
            (firebaseUserLiveData as MutableLiveData).value = FirebaseAuth.getInstance().currentUser
        }
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
            val firebaseUser = FirebaseAuth.getInstance().currentUser
            return if (resultCode == Activity.RESULT_OK && firebaseUser != null) {
                // Successfully signed in
                database.reference.child("users")
                    .child(firebaseUser.uid)
                    .setValue(User(
                        firebaseUser.uid,
                        firebaseUser.displayName ?: "",
                        firebaseUser.email,
                        firebaseUser.highQualityPhotoUri?.toString(),
                        null
                    )).addOnCompleteListener {
                        if (!it.isSuccessful) {
                            doLogout()
                            Toast.makeText(appContext, "Problem with user db registration", Toast.LENGTH_LONG).show()
                        }
                    }
                UserResult(true, null)
            } else {
                val response = IdpResponse.fromResultIntent(intent)
                // Sign in failed. If response is null the user canceled the sign-in flow using the back button.
                UserResult(false, response?.error)
            }
        }
    }

    fun doLogout() {
        AuthUI.getInstance()
            .signOut(appContext)
            .addOnCompleteListener {
                (firebaseUserLiveData as MutableLiveData).value = null
            }
    }

    private fun toUser(fUser: FirebaseUser?) = fUser?.let { firebaseUser ->
        User(
            firebaseUser.uid,
            firebaseUser.displayName ?: "",
            firebaseUser.email,
            firebaseUser.highQualityPhotoUri?.toString(),
            null
        )
    }
}