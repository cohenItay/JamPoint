package com.itaycohen.jampoint.data.models

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String? = null,
    val profileImageUrl: String? = null,
    val mainInstrument: String? = null,
    val characterizesImageUri: Uri? = null
) : Parcelable