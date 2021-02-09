package com.itaycohen.jampoint.ui.jam_team.vh

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.forEachIndexed
import androidx.core.view.isVisible
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.local.TeamItemPastMeetings
import com.itaycohen.jampoint.databinding.JamTeamPastMeetingsBinding
import com.itaycohen.jampoint.databinding.MeetingItemBinding
import com.itaycohen.jampoint.utils.LocationUtils
import com.itaycohen.jampoint.utils.UiUtils
import com.itaycohen.jampoint.utils.UiUtils.convertDpToPx

class TeamPastMeetingsViewHolder(v: View) : JamTeamBaseHolder(v) {


    private val binding = JamTeamPastMeetingsBinding.bind(v)
    private val inflater = LayoutInflater.from(v.context)

    fun bindViewHolder(item: TeamItemPastMeetings) = with(binding) {
        UiUtils.syncViewGroupChildToAmount(pastJamsContainer, item.pastMeetings.size) { toIndex ->
            val meetingView = inflater.inflate(R.layout.meeting_item, pastJamsContainer, false)
            if (toIndex != item.pastMeetings.size-1) {
                val newParams = LinearLayout.LayoutParams(meetingView.layoutParams)
                newParams.bottomMargin = root.resources.convertDpToPx(16).toInt()
                meetingView.layoutParams = newParams
            }
            pastJamsContainer.addView(meetingView)
        }
        bindInnerItems(item)
    }

    private fun bindInnerItems(item: TeamItemPastMeetings) = with (binding) {
        pastJamsContainer.forEachIndexed { index, view ->
            val childItem = item.pastMeetings[index]
            with (MeetingItemBinding.bind(view)) {
                joinBtn.isVisible = false
                val addressText = childItem.toLocation()?.let { LocationUtils.getCompleteAddressString(root.context, it) }
                addressTextInputEditText.setText(addressText, TextView.BufferType.NORMAL)
                timeTextInputEditText.setText(childItem.getUiTime(), TextView.BufferType.NORMAL)
            }
        }
    }
}