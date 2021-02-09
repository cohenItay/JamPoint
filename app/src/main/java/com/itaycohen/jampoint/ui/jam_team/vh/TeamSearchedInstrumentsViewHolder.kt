package com.itaycohen.jampoint.ui.jam_team.vh

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.forEachIndexed
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.local.TeamItemSearchedInstruments
import com.itaycohen.jampoint.databinding.JamTeamSearchedInstrumentsBinding
import com.itaycohen.jampoint.utils.UiUtils
import com.itaycohen.jampoint.utils.UiUtils.convertDpToPx

class TeamSearchedInstrumentsViewHolder(
    view: View,
    onMembershipClick: (adapPos: Int) -> Unit
) : JamTeamBaseHolder(view) {

    private val binding = JamTeamSearchedInstrumentsBinding.bind(view)
    private val inflater = LayoutInflater.from(view.context)

    init {
        binding.askToJoinBtn.setOnClickListener{ onMembershipClick(adapterPosition) }
    }

    fun bindViewHolder(item: TeamItemSearchedInstruments) = with(binding) {
        UiUtils.syncViewGroupChildToAmount(insrumentsContaienr, item.searchedInstruments.size) { toIndex ->
            val view = inflater.inflate(R.layout.instrument_text_view, insrumentsContaienr, false)
            if (toIndex != item.searchedInstruments.size-1) {
                val newParams = LinearLayout.LayoutParams(view.layoutParams)
                newParams.bottomMargin = root.resources.convertDpToPx(4).toInt()
                view.layoutParams = newParams
            }
            insrumentsContaienr.addView(view)
        }
        bindInnerItems(item)
        binding.askToJoinBtn.text = itemView.context.getString(
            if (item.isMembershipPending) R.string.cancel_request else R.string.ask_to_join_team
        )
    }

    private fun bindInnerItems(item: TeamItemSearchedInstruments) = with (binding) {
        insrumentsContaienr.forEachIndexed { index, view ->
            view as TextView
            view.text = item.searchedInstruments[index]
        }
    }
}
