package com.itaycohen.jampoint.data.repositories

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.database.*
import com.itaycohen.jampoint.data.models.*
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
                            (jamMeet.toLocation()?.let { l -> currentLocation.distanceTo(l) / 1000 <= radiusKm } ?: false)
                    if (keep) jamMeet else null
                } catch (e: DateTimeParseException) {
                    null
                }
            }
            filteredMeeting != null
        }
        return@withContext a
    }

    suspend fun updateMembership(
        user: User,
        toJamPointId: String,
        shouldJoin: Boolean
    ) = suspendCoroutine<Unit>{ continuation ->
        val ref = database.reference
            .child("jams")
            .child(toJamPointId)
            .child("pendingMembers")
        val task = if (shouldJoin) {
            ref.updateChildren(mapOf(user.id to true))
        } else {
            ref.updateChildren(mapOf(user.id to null))
        }
        task.addOnCompleteListener {
            continuation.resumeWith(
                if (it.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(it.exception!!)
                }
            )
        }
    }

    suspend fun updateMeetingParticipationFor(
        user: User,
        toJamPointId: String,
        jamMeet: JamMeet,
        shouldJoin: Boolean
    ) = suspendCoroutine<Unit>{ continuation ->
        val ref = database.reference
            .child("jams")
            .child(toJamPointId)
            .child("pendingJoinMeeting")
            .child(jamMeet.utcTimeStamp!!.split(".")[0])
        val task = if (shouldJoin) {
            ref.updateChildren(mapOf(user.id to true))
        } else {
            ref.updateChildren(mapOf(user.id to null))
        }
        task.addOnCompleteListener {
            continuation.resumeWith(
                if (it.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(it.exception!!)
                }
            )
        }
    }


    private fun observeJamPointsData() {
        database.reference.child("jams").apply {
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

    companion object {
        private val TAG = JamPlacesRepository::class.simpleName
    }
}
