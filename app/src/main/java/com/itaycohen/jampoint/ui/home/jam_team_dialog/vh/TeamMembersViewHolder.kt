package com.itaycohen.jampoint.ui.home.jam_team_dialog.vh

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.local.TeamItemMembers
import com.itaycohen.jampoint.databinding.JamTeamProfilesBinding

class TeamMembersViewHolder(v: View) : JamTeamBaseHolder(v) {

    val binding = JamTeamProfilesBinding.bind(v)
    var item: TeamItemMembers? = null

    init {
        binding.profilesRecyclerView.apply {
            adapter = ProfilesAdapter(item?.members ?: listOf())
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.HORIZONTAL).apply {
                setDrawable(ContextCompat.getDrawable(context, R.drawable.transparent_rectangle_32dp_height)!!)
            })
        }
    }

    fun bindViewHolder(item: TeamItemMembers) {
        this.item = item
        (binding.profilesRecyclerView.adapter as ProfilesAdapter).apply {
            members = item.members
            notifyDataSetChanged()
        }
    }
}