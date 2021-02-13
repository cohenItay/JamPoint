package com.itaycohen.jampoint.ui.jam_team

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.Marker

class JamTeamMutualViewModel : ViewModel() {

    val activeJamIdLiveData: LiveData<String?> = MutableLiveData(null)
    var joinTeamResult: ((reqKey: String, b: Bundle) -> Unit)? = null

    fun onMarkerClick(marker: Marker) : Boolean {
        val jamId = marker.tag as? String
        (activeJamIdLiveData as MutableLiveData).value = jamId
        return !jamId.isNullOrBlank()
    }
}