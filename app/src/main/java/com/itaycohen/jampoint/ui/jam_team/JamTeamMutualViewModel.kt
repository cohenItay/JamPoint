package com.itaycohen.jampoint.ui.jam_team

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.Marker

class JamTeamMutualViewModel : ViewModel() {

    val activeJamIdLiveData: LiveData<String?> = MutableLiveData(null)

    fun onMarkerClick(marker: Marker) : Boolean {
        val jamId = marker.tag as? String
        (activeJamIdLiveData as MutableLiveData).value = jamId
        return !jamId.isNullOrBlank()
    }
}