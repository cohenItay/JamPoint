package com.itaycohen.jampoint.ui.home.jam_team_dialog.vh

import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.local.TeamItemName
import com.itaycohen.jampoint.databinding.JamTeamNickNameBinding
import com.itaycohen.jampoint.utils.GlideApp

class TeamNameViewHolder(v: View) : JamTeamBaseHolder(v) {

    val binding = JamTeamNickNameBinding.bind(v)

    fun bindViewHolder(item: TeamItemName) {
        binding.teamNickName.text = item.teamName
        if (item.isLive) {
            Glide.with(binding.root.context)
                .asGif()
                .load(R.raw.music_note)
                .into(binding.liveImageView)
        } else
            binding.liveImageView.setImageResource(0)
        binding.jamNowTextView.isVisible = item.isLive
    }
}