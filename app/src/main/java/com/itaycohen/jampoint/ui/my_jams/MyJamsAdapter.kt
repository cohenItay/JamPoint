package com.itaycohen.jampoint.ui.my_jams

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.itaycohen.jampoint.data.models.Jam
import com.itaycohen.jampoint.databinding.MyJamItemBinding

class MyJamsAdapter : RecyclerView.Adapter<MyJamViewHolder>() {

    private lateinit var inflater: LayoutInflater
    var myJamsList = listOf<Jam>()

    override fun getItemCount() = myJamsList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyJamViewHolder {
        if (!::inflater.isInitialized)
            inflater = LayoutInflater.from(parent.context)
        return MyJamViewHolder(MyJamItemBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: MyJamViewHolder, position: Int) {
        holder.bind(myJamsList[position])
    }
}
