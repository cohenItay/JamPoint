package com.itaycohen.jampoint.data.models

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.itaycohen.jampoint.utils.DateUtils
import com.itaycohen.jampoint.utils.toLocation
import kotlinx.parcelize.Parcelize

@Parcelize
data class JamMeet(

    val latitude: Double? = null,

    val longitude: Double? = null,

    val utcTimeStamp: String? = null
) : Parcelable {

    fun toLatLng() =
        if (this.latitude!= null && this.longitude != null)
            LatLng(this.latitude, this.longitude)
        else
            null

    fun toLocation() = toLatLng()?.toLocation()

    fun getUiTime() = utcTimeStamp?.let { DateUtils.utcTimeToUiLocaleTime(it) }
}