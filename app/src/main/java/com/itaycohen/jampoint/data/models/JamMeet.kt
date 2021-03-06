package com.itaycohen.jampoint.data.models

import android.os.Parcelable
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude
import com.itaycohen.jampoint.utils.DateTimeUtils
import com.itaycohen.jampoint.utils.toLocation
import kotlinx.parcelize.Parcelize

@Parcelize
data class JamMeet(

    val id: String? = null,

    val latitude: Double? = null,

    val longitude: Double? = null,

    val utcTimeStamp: String? = null,

    val pendingMembers: Map<String, User>? = null,

    val approvedMembers: Map<String, User>? = null
) : Parcelable {

    fun toLatLng() =
        if (this.latitude!= null && this.longitude != null)
            LatLng(this.latitude, this.longitude)
        else
            null

    fun toLocation() = toLatLng()?.toLocation()

    @Exclude
    fun getUiTime() = utcTimeStamp?.let { DateTimeUtils.utcTimeToUiLocaleTime(it) }
}