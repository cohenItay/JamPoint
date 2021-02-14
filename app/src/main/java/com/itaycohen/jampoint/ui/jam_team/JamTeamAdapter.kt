package com.itaycohen.jampoint.ui.jam_team

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.local.*
import com.itaycohen.jampoint.databinding.JamTeamNickNameBinding
import com.itaycohen.jampoint.ui.jam_team.vh.*

class JamTeamAdapter(
    viewLifeCycleOwner: LifecycleOwner,
    val jamTeamViewModel: JamTeamViewModel,
    private val navController: NavController,
    private val childFragmentManager: FragmentManager
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
        is TeamItemCreateJamMeet -> R.layout.create_meeting_layout
        is TeamItemFutureMeetings -> R.layout.jam_team_future_meetings
        is TeamItemPastMeetings -> R.layout.jam_team_past_meetings
        else -> throw IllegalAccessException("Make sure that the data layer filters unsupported type.")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JamTeamBaseHolder {
        if (!::inflater.isInitialized)
            inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(viewType, parent, false)
        return when (viewType) {
            R.layout.jam_team_nick_name -> {
                val b = JamTeamNickNameBinding.bind(view)
                b.isLiveBtn.setOnClickListener { jamTeamViewModel.onLiveBtnClick(it as MaterialButton) }
                TeamNameViewHolder(b)
            }
            R.layout.jam_team_searched_instruments -> TeamSearchedInstrumentsViewHolder(
                view,
                jamTeamViewModel::updateJamTeamRequiredUsers,
                { jamTeamViewModel.onParticipateRequestClick(navController, null) },
                { user, isConfirmed -> jamTeamViewModel.updateMembershipConfirmation(user, isConfirmed)}
            )
            R.layout.create_meeting_layout -> TeamCreateJamMeetViewHolder(view, childFragmentManager, jamTeamViewModel)
            R.layout.jam_team_profiles -> TeamMembersViewHolder(view)
            R.layout.jam_team_future_meetings -> TeamFutureMeetingsViewHolder(
                view,
                jamTeamViewModel,
                childFragmentManager,
                navController
            )
            R.layout.jam_team_past_meetings -> TeamPastMeetingsViewHolder(view)
            else -> throw IllegalAccessException("Make sure that the data layer filters unsupported type.")
        }
    }

    override fun onBindViewHolder(holder: JamTeamBaseHolder, position: Int) {
        val isInEditMode: Boolean = jamTeamViewModel.isInEditModeLiveData.value!!
        when (val item = teamItemItems[position]) {
            is TeamItemName -> (holder as TeamNameViewHolder).bindViewHolder(item, isInEditMode)
            is TeamItemMembers -> (holder as TeamMembersViewHolder).bindViewHolder(item)
            is TeamItemSearchedInstruments -> (holder as TeamSearchedInstrumentsViewHolder).bindViewHolder(item, isInEditMode)
            is TeamItemCreateJamMeet -> (holder as TeamCreateJamMeetViewHolder).bindViewHolder(item, isInEditMode)
            is TeamItemFutureMeetings -> (holder as TeamFutureMeetingsViewHolder).bindViewHolder(item, isInEditMode)
            is TeamItemPastMeetings -> (holder as TeamPastMeetingsViewHolder).bindViewHolder(item)
            else -> throw IllegalAccessException("Make sure that the data layer filters unsupported type.")
        }
    }
}
