package com.itaycohen.jampoint.ui.jam_team.vh

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.forEachIndexed
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.local.TeamItemFutureMeetings
import com.itaycohen.jampoint.databinding.JamTeamFutureMeetingsBinding
import com.itaycohen.jampoint.databinding.MeetingItemBinding
import com.itaycohen.jampoint.utils.LocationUtils
import com.itaycohen.jampoint.utils.UiUtils
import com.itaycohen.jampoint.utils.UiUtils.convertDpToPx

class TeamFutureMeetingsViewHolder(
    v: View,
    private val onJoinMeetingClick: (v: View, adapterPosition: Int) -> Unit
) : JamTeamBaseHolder(v) {

    private val binding = JamTeamFutureMeetingsBinding.bind(v)
    private val inflater = LayoutInflater.from(v.context)

    fun bindViewHolder(item: TeamItemFutureMeetings) = with(binding) {
        UiUtils.syncViewGroupChildToAmount(futureJamsContainer, item.futureMeetings.size) { toIndex ->
            val view = inflater.inflate(R.layout.meeting_item, futureJamsContainer, false)
            val binding = MeetingItemBinding.bind(view)
            binding.joinBtn.setOnClickListener {
                onJoinMeetingClick(it, toIndex)
            }
            if (toIndex != item.futureMeetings.size-1) {
                val newParams = LinearLayout.LayoutParams(view.layoutParams)
                newParams.bottomMargin = root.resources.convertDpToPx(16).toInt()
                view.layoutParams = newParams
            }
            futureJamsContainer.addView(view)
        }
        bindInnerItems(item)
    }

    private fun bindInnerItems(item: TeamItemFutureMeetings) = with (binding) {
        futureJamsContainer.forEachIndexed { index, view ->
            val childItem = item.futureMeetings[index]
            with (MeetingItemBinding.bind(view)) {
                val addressText = childItem.toLocation()?.let { LocationUtils.getCompleteAddressString(root.context, it) }
                addressTextInputEditText.setText(addressText, TextView.BufferType.NORMAL)
                timeTextInputEditText.setText(childItem.getUiTime(), TextView.BufferType.NORMAL)
                joinBtn.isEnabled = !item.isMembershipPending
                joinBtn.text = root.context.getString(
                    if (!item.isPendingForList[index])
                        R.string.ask_to_join2
                    else
                        R.string.cancel_request2
                )
            }
        }
    }
}