package com.itaycohen.jampoint.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String? = null,
    val profileImageUrl: String? = null,
    val mainInstrument: String? = null
) : Parcelable