package com.itaycohen.jampoint.data.models.local

import com.itaycohen.jampoint.data.models.JamMeet

class TeamItemFutureMeetings(
    val futureMeetings: List<JamMeet>,
    /**
     * Same size as [futureMeetings], indicates whether this connected user is pending to join that meeting
     */
    val futureMeetingsSelfPendingList: List<Boolean>,
    val membershipState: MembershipState
) : TeamItemModel
