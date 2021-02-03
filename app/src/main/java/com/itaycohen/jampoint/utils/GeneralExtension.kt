package com.itaycohen.jampoint.utils

import android.location.Location
import android.net.Uri
import androidx.core.net.toUri
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.itaycohen.jampoint.data.models.JamMeet


fun LatLng.toLocation() = Location("No provider").apply {
    longitude = this@toLocation.longitude
    latitude = this@toLocation.latitude
}

fun Location.toLatLng() = LatLng(this.latitude, this.longitude)

fun Place.toLocation() = this.latLng?.toLocation()

val FirebaseUser.highQualityPhotoUri: Uri?
    get() {
        val atry = this.photoUrl?.authority
        val provId = when {
            atry?.contains("google")==true -> GoogleAuthProvider.PROVIDER_ID
            atry?.contains("facebook")==true -> FacebookAuthProvider.PROVIDER_ID
            else -> null
        }
        return when (provId) {
            FirebaseAuthProvider.PROVIDER_ID -> {
                null
            }
            FacebookAuthProvider.PROVIDER_ID -> {
                photoUrl?.buildUpon()?.appendQueryParameter("height", "500")?.build()
            }
            GoogleAuthProvider.PROVIDER_ID -> {
                photoUrl?.toString()?.replace("s96-c", "s480-c")?.toUri()
            }
            else -> null
        }
    }