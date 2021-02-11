package com.itaycohen.jampoint.data.models

import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.database.PropertyName
import com.itaycohen.jampoint.data.models.local.MembershipState

@IgnoreExtraProperties
data class Jam(

    val groupManagers: Map<String, Boolean?>? = null,

    @field:JvmField
    val isLive: Boolean? = null,

    @field:JvmField
    @PropertyName("nickName")
    val jamPlaceNickname: String? = null,

    val searchedInstruments: List<String>? = null,

    val members: Map<String, User>? = null,

    val jamMeetings: List<JamMeet>? = null,

    val pendingMembers: Map<String, User>? = null,

    val jamPointId: String? = null,

    val pendingJoinMeeting: Map<String, Map<String, Boolean>?>? = null
) {
    fun getMembershipStateFor(userId: String): MembershipState {
        return when {
            groupManagers?.containsKey(userId) == true -> MembershipState.Manager
            members?.values?.find { it.id == userId } != null -> MembershipState.Yes
            pendingMembers?.containsKey(userId) == true -> MembershipState.Pending
            else -> MembershipState.No
        }
    }
}