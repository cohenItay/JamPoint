package com.itaycohen.jampoint.utils

import android.content.res.Resources
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place


fun LatLng.toLocation() = Location("No provider").apply {
    longitude = this@toLocation.longitude
    latitude = this@toLocation.latitude
}

fun Location.toLatLng() = LatLng(this.latitude, this.longitude)

fun Place.toLocation() = this.latLng?.toLocation()