package com.itaycohen.jampoint.data.repositories

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.itaycohen.jampoint.data.UserResult
import com.itaycohen.jampoint.data.models.User
import com.itaycohen.jampoint.utils.highQualityPhotoUri
import kotlin.coroutines.suspendCoroutine

class UserRepository(
    private val appContext: Context,
    private val database: FirebaseDatabase
) {

    private val firebaseUserLiveData: LiveData<FirebaseUser?> = MutableLiveData(null)

    val userLiveData: LiveData<User?> =  MutableLiveData(null)

    init {
        FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth ->
            (firebaseUserLiveData as MutableLiveData).value = firebaseAuth.currentUser
            observeDatabaseUser(firebaseAuth.currentUser?.uid)
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

    suspend fun updateUserInstrument(instrument: String) = suspendCoroutine<Unit> { continuation ->
        val userId = userLiveData.value?.id
        if (userId != null) {
            database.reference
                .child("users/$userId/mainInstrument")
                .setValue(instrument).addOnCompleteListener {
                    if (it.isSuccessful) {
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        continuation.resumeWith(Result.failure(it.exception!!))
                    }
                }
        } else {
            continuation.resumeWith(Result.failure(IllegalStateException("No user is logged in")))
        }
    }

    private fun observeDatabaseUser(userId: String?) {
        userLiveData as MutableLiveData
        if (userId != null) {
            database.reference
                .child("users/$userId")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        userLiveData.value = user
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
        } else {
            userLiveData.value = null
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