package com.itaycohen.jampoint.data.repositories

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import com.itaycohen.jampoint.data.models.Jam
import com.itaycohen.jampoint.data.models.Musician
import com.itaycohen.jampoint.data.models.QueryState
import kotlinx.coroutines.*
import java.time.Instant
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine

class JamPlacesRepository(
    private val appContext: Context,
    private val database: FirebaseDatabase
) : CoroutineScope {

    val jamPlacesLiveData : LiveData<Map<String, Jam>> = MutableLiveData(mapOf())
    val musiciansLiveData:  LiveData<Map<String, Musician>> = MutableLiveData(mapOf())
    val jamPlacesQueryStateLiveData: LiveData<QueryState> = MutableLiveData(QueryState.Idle)
    override val coroutineContext: CoroutineContext = Dispatchers.Default

    init {
        observeJamPointsData()
    }

    suspend fun getJamPlacesInRadiusAndDays(
        currentLocation: Location,
        radiusKm: Int = 5,
        daysOffset: Int = 7
    ) = withContext(Dispatchers.Default) {
        val a = jamPlacesLiveData.value?.filter {
            // Check if there is at least one future jam meeting
            // And that this  meeting meets the radius requirement
            val filteredMeeting = it.value.jamMeetings?.firstOrNull()?.let { jamMeet ->
                try {
                    val meetInstant = Instant.parse(jamMeet.utcTimeStamp)
                    val nowInstant = Instant.now()
                    val keep = meetInstant.isAfter(nowInstant) &&
                            nowInstant.plus(daysOffset.toLong(), ChronoUnit.DAYS).isAfter(meetInstant) &&
                            (jamMeet.toLocation()?.let { currentLocation.distanceTo(it) / 1000 <= radiusKm } ?: false)
                    if (keep) jamMeet else null
                } catch (e: DateTimeParseException) {
                    null
                }
            }
            filteredMeeting != null
        }
        return@withContext a
    }

    suspend fun getMusiciansInfo(ids: Collection<String>) : Collection<Musician>? = suspendCoroutine { continuation ->
        val musiciansMaps = musiciansLiveData.value
        if (musiciansMaps == null) {
            continuation.resumeWith(Result.success(null))
            return@suspendCoroutine
        }
        val musiciansCollection = musiciansMaps.filterKeys { ids.contains(it) }.values
        continuation.resumeWith(Result.success(musiciansCollection))
    }

    private fun observeJamPointsData() {
        observerJamsData()
        observeMusiciansData()
    }

    private fun observerJamsData() {
        database.reference.child("jams").apply {
            get().addOnCompleteListener {
                if (it.isSuccessful) {
                    updateJamPlaces(it.result)
                }
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
        val gnti = object : GenericTypeIndicator<Map<String, Jam>>() {}
        jamPlacesLiveData.postValue(snapshot.getValue(gnti))
        jamPlacesQueryStateLiveData.postValue(QueryState.Success)
    }

    private fun observeMusiciansData() {
        database.reference.child("users").apply {
            get().addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(TAG, "observeJamPointsData: GOT MUSICIANS")
                    val gnti = object : GenericTypeIndicator<Map<String, Musician>>() {}
                    (musiciansLiveData as MutableLiveData).postValue(it.result.getValue(gnti))
                } else {
                    Log.e(TAG, "observeJamPointsData: Cannot fetch Musicians", it.exception)
                }
            }
            addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "observeJamPointsData: GOT MUSICIANS DATA CHANGE")
                    val gnti = object : GenericTypeIndicator<Map<String, Musician>>() {}
                    (musiciansLiveData as MutableLiveData).postValue(snapshot.getValue(gnti))
                }

                override fun onCancelled(error: DatabaseError) {
                    jamPlacesQueryStateLiveData as MutableLiveData
                    jamPlacesQueryStateLiveData.value = QueryState.Failure(error.message)
                }
            })
        }
    }

    companion object {
        private val TAG = JamPlacesRepository::class.simpleName
    }
}
