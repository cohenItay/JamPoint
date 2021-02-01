package com.itaycohen.jampoint.ui.home.jam_team_dialog.vh

import android.view.LayoutInflater
import android.view.View
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.local.TeamItemFutureMeetings
import com.itaycohen.jampoint.databinding.JamTeamFutureMeetingsBinding
import com.itaycohen.jampoint.utils.UiUtils

class TeamFutureMeetingsViewHolder(v: View) : JamTeamBaseHolder(v) {

    private val binding = JamTeamFutureMeetingsBinding.bind(v)
    private val inflater = LayoutInflater.from(v.context)

    fun bindViewHolder(item: TeamItemFutureMeetings) {
        UiUtils.syncViewGroupChildToAmount(binding.futureJamsContainer, item.futureMeetings.size) {
            inflater.inflate(R.layout.meeting_item, binding.futureJamsContainer, true)
        }
        bindInnerItems(item)
    }

    private fun bindInnerItems(item: TeamItemFutureMeetings) = with (binding) {

    }
}