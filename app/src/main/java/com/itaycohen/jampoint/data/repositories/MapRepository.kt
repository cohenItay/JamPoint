package com.itaycohen.jampoint.data.repositories

import android.content.Context
import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import com.itaycohen.jampoint.data.models.JamPlace
import com.itaycohen.jampoint.data.models.QueryState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MapRepository(
    private val appContext: Context,
    private val database: FirebaseDatabase
) {

    val jamPlacesLiveData : LiveData<Map<String, JamPlace>> = MutableLiveData(mapOf())
    val jamPlacesQueryStateLiveData: LiveData<QueryState> = MutableLiveData(QueryState.Idle)

    init {
        observeJamPoints()
    }

    suspend fun getJamPlacesInRadius(currentLocation: Location, radiusKm: Int) = withContext(Dispatchers.Default) {
        return@withContext jamPlacesLiveData.value?.filter {
            val lat = it.value.latitude ?: return@filter false
            val lon = it.value.longitude ?: return@filter false
            val location = Location("No provider").apply {
                latitude = lat
                longitude = lon
            }
            currentLocation.distanceTo(location)/1000 <= radiusKm
        }
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
