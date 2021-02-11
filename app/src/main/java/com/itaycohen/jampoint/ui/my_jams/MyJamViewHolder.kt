package com.itaycohen.jampoint.ui.my_jams

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.itaycohen.jampoint.data.models.Jam
import com.itaycohen.jampoint.databinding.MyJamItemBinding

class MyJamViewHolder(
    private val binding: MyJamItemBinding,

    private val onLiveBtnClick: (View, String) -> Unit,
    private val onItemClick: (View, String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private var jamPlaceId: String? = null

    init {
        binding.isLiveBtn.setOnClickListener { v -> jamPlaceId?.also { onLiveBtnClick(v, it) } }
        binding.root.setOnClickListener {  v -> jamPlaceId?.also { onItemClick(v, it) } }
    }

    fun bind(jam: Jam) {
        jamPlaceId = jam.jamPointId
        binding.jamPointTitle.text = jam.jamPlaceNickname
        binding.isLiveBtn.isChecked = jam.isLive == true
    }
}
