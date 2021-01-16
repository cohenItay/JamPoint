package com.itaycohen.jampoint.data

import com.firebase.ui.auth.FirebaseUiException
import com.google.firebase.auth.FirebaseUser

/**
 * Model which represents the login state.
 * when both of the fields are null, the user canceled the sign-in flow using the back button.
 */
data class UserResult(
    val user: FirebaseUser?,
    val error: FirebaseUiException?
)