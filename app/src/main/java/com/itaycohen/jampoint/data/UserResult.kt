package com.itaycohen.jampoint.data

import com.firebase.ui.auth.FirebaseUiException
import com.google.firebase.auth.FirebaseUser

/**
 * Model which represents the login state.
 */
data class UserResult(
    val isSuccess: Boolean,
    val error: FirebaseUiException?
)