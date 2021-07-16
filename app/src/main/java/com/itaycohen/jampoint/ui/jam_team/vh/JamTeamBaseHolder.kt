package com.itaycohen.jampoint.ui.jam_team.vh

import android.view.View
import androidx.recyclerview.widget.RecyclerView

abstract class JamTeamBaseHolder(v: View) : RecyclerView.ViewHolder(v) {

    open fun onAttachedToWindow() {}
}

