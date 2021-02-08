package com.itaycohen.jampoint.ui.find_jams.jam_team_dialog.vh

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.Musician
import com.itaycohen.jampoint.databinding.ProfileThumbBinding

class ProfilesAdapter(
    var members: List<Musician>
) : RecyclerView.Adapter<ProfilesAdapter.ProfileViewHolder>() {

    private lateinit var inflater: LayoutInflater

    override fun getItemCount() = members.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        if (!::inflater.isInitialized)
            inflater = LayoutInflater.from(parent.context)
        return ProfileViewHolder(inflater.inflate(R.layout.profile_thumb, parent, false))
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        holder.bindViewHolder(members[position])
    }

    class ProfileViewHolder(v: View) : RecyclerView.ViewHolder(v) {

        val binding = ProfileThumbBinding.bind(v)

        fun bindViewHolder(musician: Musician) {
            Glide.with(itemView)
                .load(musician.profileImageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_baseline_account_circle_24)
                .error(R.drawable.ic_baseline_account_circle_24)
                .into(binding.profileImage)
            binding.nameTextView.text = musician.fullName
            binding.mainInstrumentText.text = musician.mainInstrument
        }
    }
}


