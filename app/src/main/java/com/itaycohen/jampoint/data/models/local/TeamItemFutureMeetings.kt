package com.itaycohen.jampoint.data.models.local

import com.itaycohen.jampoint.data.models.JamMeet

class TeamItemFutureMeetings(
    val futureMeetings: List<JamMeet>,
    searchedInstruments: List<String>?
) : TeamItemModel
