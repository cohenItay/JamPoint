package com.itaycohen.jampoint.data.models.local

data class TeamItemName(
    val teamName: String,
    val isLive: Boolean,
    val isManager: Boolean
) : TeamItemModel