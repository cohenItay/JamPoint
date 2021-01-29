package com.itaycohen.jampoint.data.repositories

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import com.itaycohen.jampoint.data.models.JamPlace
import com.itaycohen.jampoint.data.models.QueryState

class MapRepository(
    private val appContext: Context,
    private val database: FirebaseDatabase
) {

    val jamPlacesLiveData : LiveData<Map<String, JamPlace>> = MutableLiveData(mapOf())
    val jamPlacesQueryStateLiveData: LiveData<QueryState> = MutableLiveData(QueryState.Idle)

    init {
        observeJamPoints()
    }

    private fun observeJamPoints() {
        database.reference.child("jams").apply {
            get().addOnCompleteListener {
                if (it.isSuccessful)
                    updateJamPlaces(it.result)
            }
            addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    updateJamPlaces(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    jamPlacesQueryStateLiveData as MutableLiveData
                    jamPlacesQueryStateLiveData.value = QueryState.Failure(error.message)
                }
            })
        }
    }

    private fun updateJamPlaces(snapshot: DataSnapshot) {
        jamPlacesLiveData as MutableLiveData
        jamPlacesQueryStateLiveData as MutableLiveData
        val gnti = object : GenericTypeIndicator<Map<String, JamPlace>>() {}
        jamPlacesLiveData.value = snapshot.getValue(gnti)
        jamPlacesQueryStateLiveData.value = QueryState.Success
    }
}
