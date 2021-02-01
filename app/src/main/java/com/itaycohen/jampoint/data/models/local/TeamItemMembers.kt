package com.itaycohen.jampoint.data.models.local

import com.itaycohen.jampoint.data.models.Musician

data class TeamItemMembers(
    val members: List<Musician>
) : TeamItemModel