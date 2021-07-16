package com.itaycohen.jampoint.ui.jam_team.vh

import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.local.TeamItemName
import com.itaycohen.jampoint.databinding.JamTeamNickNameBinding

class TeamNameViewHolder(
    private val binding: JamTeamNickNameBinding
) : JamTeamBaseHolder(binding.root) {

    fun bindViewHolder(item: TeamItemName, isInEditMode: Boolean) {
        binding.teamNickName.text = item.teamName
        binding.liveImageView.isVisible = !isInEditMode && item.isLive
        binding.jamNowTextView.isVisible = !isInEditMode && item.isLive
        binding.isLiveBtn.isVisible = isInEditMode
        binding.isLiveBtn.isChecked = item.isLive
        binding.isLiveBtn.isEnabled = item.isManager && isInEditMode
        if (!isInEditMode && item.isLive) {
            Glide.with(binding.root.context)
                .asGif()
                .load(R.raw.music_note)
                .into(binding.liveImageView)
        } else {
            binding.liveImageView.setImageResource(0)
        }
    }
}