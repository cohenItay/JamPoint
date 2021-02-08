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

    val members: List<Musician>? = null,

    val jamMeetings: List<JamMeet>? = null,

    val pendingMembers: Map<String, Boolean?>? = null,

    val pendingJoinMeeting: Map<String, Map<String, Boolean>?>? = null
) {
    val jampPointId: String?
        get() = groupManagers?.let {
            if (it.isNotEmpty() && !jamPlaceNickname.isNullOrBlank())
                "${jamPlaceNickname.replace(" ", "")}_${it[0]}"
            else
                null
        }
}