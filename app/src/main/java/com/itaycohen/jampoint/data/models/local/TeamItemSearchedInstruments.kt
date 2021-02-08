package com.itaycohen.jampoint.data.models.local

data class TeamItemSearchedInstruments(
    val searchedInstruments: List<String>,
    val isMembershipPending: Boolean
) : TeamItemModel
