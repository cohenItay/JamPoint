package com.itaycohen.jampoint.data.models.local

sealed class MembershipState {
    object Manager : MembershipState()
    object Yes : MembershipState()
    object No : MembershipState()
    object Pending : MembershipState()
}