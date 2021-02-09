package com.itaycohen.jampoint.ui.jam_team

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.local.*
import com.itaycohen.jampoint.ui.jam_team.vh.*

class JamTeamAdapter(
    viewLifeCycleOwner: LifecycleOwner,
    val jamTeamViewModel: JamTeamViewModel,
    private val navController: NavController
) : RecyclerView.Adapter<JamTeamBaseHolder>() {

    private var teamItemItems: List<TeamItemModel> = jamTeamViewModel.teamItemsLiveData.value ?: listOf()
    private lateinit var inflater: LayoutInflater

    init {
        jamTeamViewModel.teamItemsLiveData.observe(viewLifeCycleOwner) {
            teamItemItems = it
            notifyDataSetChanged()
        }
    }

    override fun getItemCount() = teamItemItems.size

    override fun getItemViewType(position: Int) = when (teamItemItems[position]) {
        is TeamItemName -> R.layout.jam_team_nick_name
        is TeamItemMembers -> R.layout.jam_team_profiles
        is TeamItemSearchedInstruments -> R.layout.jam_team_searched_instruments
        is TeamItemFutureMeetings -> R.layout.jam_team_future_meetings
        is TeamItemPastMeetings -> R.layout.jam_team_past_meetings
        else -> throw IllegalAccessException("Make sure that the data layer filters unsupported type.")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JamTeamBaseHolder {
        if (!::inflater.isInitialized)
            inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.jam_team_nick_name -> TeamNameViewHolder(view)
            R.layout.jam_team_profiles -> TeamMembersViewHolder(view)
            R.layout.jam_team_searched_instruments -> TeamSearchedInstrumentsViewHolder(view) {
                jamTeamViewModel.onParticipateRequestClick(navController, null)
            }
            R.layout.jam_team_future_meetings -> TeamFutureMeetingsViewHolder(view) { _, i ->
                jamTeamViewModel.onParticipateRequestClick(navController, i)
            }
            R.layout.jam_team_past_meetings -> TeamPastMeetingsViewHolder(view)
            else -> throw IllegalAccessException("Make sure that the data layer filters unsupported type.")
        }
    }

    override fun onBindViewHolder(holder: JamTeamBaseHolder, position: Int) {
        when (val item = teamItemItems[position]) {
            is TeamItemName -> (holder as TeamNameViewHolder).bindViewHolder(item)
            is TeamItemMembers -> (holder as TeamMembersViewHolder).bindViewHolder(item)
            is TeamItemSearchedInstruments -> (holder as TeamSearchedInstrumentsViewHolder).bindViewHolder(item)
            is TeamItemFutureMeetings -> (holder as TeamFutureMeetingsViewHolder).bindViewHolder(item)
            is TeamItemPastMeetings -> (holder as TeamPastMeetingsViewHolder).bindViewHolder(item)
            else -> throw IllegalAccessException("Make sure that the data layer filters unsupported type.")
        }
    }
}
