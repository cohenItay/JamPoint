package com.itaycohen.jampoint.data.models

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import com.google.gson.annotations.SerializedName

@IgnoreExtraProperties
data class JamPlace(

    val groupManagers: List<String>? = null,

    @field:JvmField
    val isLive: Boolean? = null,

    val latitude: Double? = null,

    val longitude: Double? = null,

    val membersIds: List<String>? = null,

    val membersNames: List<String>? = null,

    @field:JvmField
    @PropertyName("nickName")
    val jamPlaceNickname: String? = null,

    val searchedInstruments: List<String>? = null
)