package com.itaycohen.jampoint.data.models.local

import com.itaycohen.jampoint.data.models.User

data class TeamItemMembers(
    val members: List<User>
) : TeamItemModel