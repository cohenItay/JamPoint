package com.itaycohen.jampoint.ui.jam_team.vh

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.forEachIndexed
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.User
import com.itaycohen.jampoint.data.models.local.MembershipState
import com.itaycohen.jampoint.data.models.local.MembershipState.Pending
import com.itaycohen.jampoint.data.models.local.TeamItemSearchedInstruments
import com.itaycohen.jampoint.databinding.ApprovePendingUserItemBinding
import com.itaycohen.jampoint.databinding.JamTeamSearchedInstrumentsBinding
import com.itaycohen.jampoint.utils.UiUtils
import com.itaycohen.jampoint.utils.UiUtils.convertDpToPx

class TeamSearchedInstrumentsViewHolder(
    view: View,
    private val saveNewInstruments: (list: List<String>) -> Unit,
    onMembershipClick: (adapPos: Int) -> Unit,
    private val onMembershipRequest: (user: User, isConfirmed: Boolean) -> Unit
) : JamTeamBaseHolder(view) {

    private val binding = JamTeamSearchedInstrumentsBinding.bind(view)
    private val inflater = LayoutInflater.from(view.context)

    init {
        binding.askToJoinBtn.setOnClickListener{ onMembershipClick(adapterPosition) }
        binding.editInstrumentsBtn.setOnClickListener {
            val textInputLayout = LayoutInflater
                .from(binding.root.context)
                .inflate(R.layout.simple_text_input, null) as TextInputLayout
            textInputLayout.editText?.setText(it.tag as? String)
            MaterialAlertDialogBuilder(itemView.context)
                .setTitle(R.string.edit_instruments)
                .setView(textInputLayout)
                .setCancelable(false)
                .setPositiveButton(R.string.update_) { _, _ ->
                    val newInstruments = textInputLayout.editText!!.text.toString().split(",").map {
                        it.trim()
                    }
                    saveNewInstruments(newInstruments)
                }
                .setNegativeButton(R.string.back, null)
                .show()
        }
    }

    fun bindViewHolder(item: TeamItemSearchedInstruments, isInEditMode: Boolean) = with(binding) {
        UiUtils.syncViewGroupChildToAmount(insrumentsContaienr, item.searchedInstruments.size) { toIndex ->
            val view = inflater.inflate(R.layout.instrument_text_view, insrumentsContaienr, false)
            if (toIndex != item.searchedInstruments.size-1) {
                val newParams = LinearLayout.LayoutParams(view.layoutParams)
                newParams.bottomMargin = root.resources.convertDpToPx(4).toInt()
                view.layoutParams = newParams
            }
            insrumentsContaienr.addView(view)
        }
        UiUtils.syncViewGroupChildToAmount(pendingsContaienr, item.pendingUsers.size) { toIndex ->
            val view = inflater.inflate(R.layout.approve_pending_user_item, pendingsContaienr, false)
            if (toIndex != item.pendingUsers.size-1) {
                val newParams = LinearLayout.LayoutParams(view.layoutParams)
                newParams.bottomMargin = root.resources.convertDpToPx(4).toInt()
                view.layoutParams = newParams
            }
            pendingsContaienr.addView(view)
        }
        val pendingVisible = item.pendingUsers.isNotEmpty() && item.membershipState == MembershipState.Manager && isInEditMode
        pendingsContaienr.isVisible = pendingVisible
        pendinggTitle.isVisible = pendingVisible
        val isMember = item.membershipState == MembershipState.Yes || item.membershipState == MembershipState.Manager
        binding.askToJoinBtn.isVisible = !pendingVisible
        binding.askToJoinBtn.isEnabled = !isMember
        binding.askToJoinBtn.text = binding.root.context.getString(
            if (isMember) R.string.jam_member
            else if (item.membershipState == Pending) R.string.cancel_request
            else  R.string.ask_to_join_team
        )
        binding.editInstrumentsBtn.tag = item.searchedInstruments.joinToString()
        binding.editInstrumentsBtn.isVisible = isInEditMode

        bindInnerItems(item)
    }

    private fun bindInnerItems(item: TeamItemSearchedInstruments) = with (binding) {
        insrumentsContaienr.forEachIndexed { index, view ->
            view as TextView
            view.text = item.searchedInstruments[index]
        }
        pendingsContaienr.forEachIndexed { index, view ->
            with (ApprovePendingUserItemBinding.bind(view)) {
                val user = item.pendingUsers[index]
                personNameTextView.text = user.fullName
                personInstrumentTextView.text = user.mainInstrument
                personInstrumentTextView.isVisible = !user.mainInstrument.isNullOrBlank()
                Glide.with(view)
                    .load(user.profileImageUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_baseline_account_circle_24)
                    .error(R.drawable.ic_baseline_account_circle_24)
                    .into(smallProfileImageView)
                confirmBtn.setOnClickListener {
                    onMembershipRequest(user, true)
                }
                declineBtn.setOnClickListener{
                    onMembershipRequest(user, false)
                }
            }
        }
    }
}
