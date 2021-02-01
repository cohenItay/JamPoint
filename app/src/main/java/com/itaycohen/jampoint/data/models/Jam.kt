package com.itaycohen.jampoint.data.models

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import com.google.gson.annotations.SerializedName

@IgnoreExtraProperties
data class Jam(

    val groupManagers: List<String>? = null,

    @field:JvmField
    val isLive: Boolean? = null,

    @field:JvmField
    @PropertyName("nickName")
    val jamPlaceNickname: String? = null,

    val searchedInstruments: List<String>? = null,

    val membersIds: List<String>? = null,

    val jamMeetings: List<JamMeet>? = null,

    val musicians: List<Musician>? = null
) {

    constructor(
        groupManagers: List<String>? = null,
        isLive: Boolean? = null,
        membersIds: List<String>? = null,
        jamPlaceNickname: String? = null,
        searchedInstruments: List<String>? = null,
        jamMeetings: List<JamMeet>? = null,
    ) : this (
        groupManagers,
        isLive,
        jamPlaceNickname,
        searchedInstruments,
        membersIds,
        jamMeetings,
        null
    )
}