package com.itaycohen.jampoint.data.models.local

import com.itaycohen.jampoint.data.models.User

data class TeamItemSearchedInstruments(
    val searchedInstruments: List<String>,
    val pendingUsers: List<User>,
    val membershipState: MembershipState
) : TeamItemModel