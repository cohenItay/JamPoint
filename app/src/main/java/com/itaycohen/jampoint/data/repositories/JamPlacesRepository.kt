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
        daysOffset: Int = 14
    ) = withContext(Dispatchers.Default) {
        return@withContext jamPlacesLiveData.value?.filter {
            // Check if there is at least one future jam meeting
            // And that this  meeting meets the radius requirement
            val filteredMeeting = it.value.jamMeetings?.values?.find{ jamMeet ->
                try {
                    val meetInstant = Instant.parse(jamMeet.utcTimeStamp)
                    val nowInstant = Instant.now()
                    val keep = meetInstant.isAfter(nowInstant) &&
                            nowInstant.plus(daysOffset.toLong(), ChronoUnit.DAYS).isAfter(meetInstant) &&
                            (jamMeet.toLocation()?.let { l -> currentLocation.distanceTo(l) / 1000 <= radiusKm } ?: false)
                    keep
                } catch (e: DateTimeParseException) {
                    false
                }
            }
            filteredMeeting != null
        }

    }

    suspend fun jamPointMembershipRequest(
        user: User,
        toJamPointId: String,
        shouldJoin: Boolean
    ) = suspendCoroutine<Unit>{ continuation ->
        val ref = database.reference
            .child("jams/$toJamPointId/pendingMembers/${user.id}")
        val task = if (shouldJoin) {
            ref.setValue(user)
        } else {
            ref.removeValue()
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

    suspend fun jamPointMembershipAnswer(
        user: User,
        toJamPointId: String,
        confirmed: Boolean
    ) = suspendCoroutine<Unit>{ continuation ->
        if (confirmed) {
            database.reference
                .child("jams/$toJamPointId")
                .runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val jam = currentData.getValue(Jam::class.java) ?: return Transaction.abort()
                        val newPendingMap = jam.pendingMembers?.toMutableMap()
                        newPendingMap?.remove(user.id)
                        val newMembersMap = jam.members?.toMutableMap()
                        newMembersMap?.put(user.id, user)
                        val newJam = jam.copy(
                            pendingMembers =  newPendingMap,
                            members = newMembersMap
                        )
                        currentData.value = newJam
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        if (committed || error == null) {
                            continuation.resumeWith(Result.success(Unit))
                        } else {
                            continuation.resumeWith(Result.failure(error.toException()))
                        }
                    }

                })
        } else {
            database.reference
                .child("jams/$toJamPointId/pendingMembers/${user.id}")
                .removeValue { error, ref ->
                    if (error == null) {
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        continuation.resumeWith(Result.failure(error.toException()))
                    }
                }
        }
    }

    suspend fun jamPointJoinMeetingAnswer(
        futureMeet: JamMeet,
        user: User,
        toJamPointId: String,
        isConfirmed: Boolean
    ) = suspendCoroutine<Unit>{ continuation ->
        if (isConfirmed) {
            database.reference
                .child("jams/$toJamPointId/jamMeetings/${futureMeet.id}")
                .runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {

                        val jamMeet = currentData.getValue(JamMeet::class.java) ?: return Transaction.abort()
                        val newPendingMembersMap = jamMeet.pendingMembers?.toMutableMap() ?: return Transaction.abort()
                        newPendingMembersMap.remove(user.id)

                        val newApprovedMembersMap = jamMeet.approvedMembers?.toMutableMap() ?: mutableMapOf()
                        newApprovedMembersMap[user.id] = user

                        val newJamMeet = jamMeet.copy(
                            pendingMembers = newPendingMembersMap,
                            approvedMembers = newApprovedMembersMap
                        )
                        currentData.value = newJamMeet
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        if (committed || error == null) {
                            continuation.resumeWith(Result.success(Unit))
                        } else {
                            continuation.resumeWith(Result.failure(error.toException()))
                        }
                    }

                })
        } else {
            database.reference
                .child("jams/$toJamPointId/jamMeetings/${futureMeet.id}/pendingMembers/${user.id}")
                .removeValue { error, ref ->
                    if (error == null) {
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        continuation.resumeWith(Result.failure(error.toException()))
                    }
                }
        }
    }

    suspend fun removeUserFromMeeting(
        futureMeet: JamMeet,
        user: User,
        toJamPointId: String
    ) = suspendCoroutine<Unit> { continuation ->
        database.reference
            .child("jams/$toJamPointId/jamMeetings/${futureMeet.id}/approvedMembers/${user.id}")
            .removeValue { error, ref ->
                if (error == null) {
                    continuation.resumeWith(Result.success(Unit))
                } else {
                    continuation.resumeWith(Result.failure(error.toException()))
                }
            }
    }

    suspend fun updateMeetingParticipateRequestFor(
        user: User,
        toJamPointId: String,
        jamMeet: JamMeet,
        wantToParticipate: Boolean
    ) = suspendCoroutine<Unit>{ continuation ->
        val ref = database.reference
            .child("jams/$toJamPointId/jamMeetings/${jamMeet.id}/pendingMembers")
        val task = if (wantToParticipate) {
            ref.updateChildren(mapOf(user.id to user))
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

    /**
     * Fetches the Jam models which the [userId] owns / manages.
     * @throws [DatabaseError]
     */
    suspend fun fetchSelfJams(userId: String) = suspendCoroutine<Map<String, Jam>?> {  continuation ->
        database.reference
            .child("jams")
            .orderByChild("groupManagers/$userId")
            .equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val gnti = object : GenericTypeIndicator<Map<String, Jam>>() {}
                    continuation.resumeWith(Result.success(snapshot.getValue(gnti)))
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWith(Result.failure(error.toException()))
                }
            })
    }

    /**
     * Fetches the Jam models which the [userId] owns / manages.
     * @throws [DatabaseError]
     */
    suspend fun fetchJam(jamPointId: String) = suspendCoroutine<Jam?> {  continuation ->
        database.reference
            .child("jams")
            .orderByKey()
            .equalTo(jamPointId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    continuation.resumeWith(Result.success(snapshot.children.first().getValue(Jam::class.java)))
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWith(Result.failure(error.toException()))
                }
            })
    }

    suspend fun updateIsLive(jamPointId: String, isLive: Boolean) = suspendCoroutine<Unit> { continuation ->
        database.reference
            .child("jams/$jamPointId")
            .updateChildren(mapOf("isLive" to isLive)) { error, ref ->
                if (error == null) {
                    continuation.resumeWith(Result.success(Unit))
                } else {
                    continuation.resumeWith(Result.failure(error.toException()))
                }
            }
    }

    suspend fun updateJamTeamRequiredUsers(
        jamPointId: String,
        newInstruments: List<String>
    ) = suspendCoroutine<Unit> { continuation ->
        database.reference
            .child("jams/$jamPointId/searchedInstruments")
            .setValue(newInstruments) { error, ref ->
                if (error == null) {
                    continuation.resumeWith(Result.success(Unit))
                } else {
                    continuation.resumeWith(Result.failure(error.toException()))
                }
            }
    }

    suspend fun createNewJamPoint(nickName: String, manager: User) = suspendCoroutine<Jam> { continuation ->
        val jamPointId = "${nickName.replace(" ", "_")}_${manager.id}"
        val jam = Jam(
            groupManagers = mapOf(manager.id to true),
            isLive = false,
            jamPlaceNickname = nickName,
            searchedInstruments = listOf(),
            members = mapOf(manager.id to manager),
            jamPointId = jamPointId
        )
        database.reference
            .child("jams/$jamPointId")
            .setValue(jam) { error, ref ->
                if (error == null) {
                    continuation.resumeWith(Result.success(jam))
                } else {
                    continuation.resumeWith(Result.failure(error.toException()))
                }
            }
    }

    suspend fun updateMeetingTime(futureMeet: JamMeet, jamId: String, utcTimeStmap: String) = suspendCoroutine<Unit> { continuation ->
        database.reference
            .child("jams/$jamId/jamMeetings/${futureMeet.id}/utcTimeStamp")
            .setValue(utcTimeStmap){ error, ref ->
                if (error == null) {
                    continuation.resumeWith(Result.success(Unit))
                } else {
                    continuation.resumeWith(Result.failure(error.toException()))
                }
            }
    }

    suspend fun updateMeetingPlace(futureMeet: JamMeet, jamId: String, location: Location) = suspendCoroutine<Unit> { continuation ->
        database.reference
            .child("jams/$jamId/jamMeetings/${futureMeet.id}")
            .runTransaction(object: Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val jamMeet = currentData.getValue(JamMeet::class.java) ?: return Transaction.abort()
                    val newMeet = jamMeet.copy(
                        longitude = location.longitude,
                        latitude = location.latitude
                    )
                    currentData.value = newMeet
                    return Transaction.success(currentData)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                    if (committed || error == null) {
                        continuation.resumeWith(Result.success(Unit))
                    } else {
                        continuation.resumeWith(Result.failure(error.toException()))
                    }
                }
            })
    }

    suspend fun createNewJamMeet(pointId: String, jamMeet: JamMeet) = suspendCoroutine<Unit> { continuation ->
        val ref = database.reference
            .child("jams/$pointId/jamMeetings")
            .push()
        val meetId = ref.key
        if (meetId == null) {
            val exception = NullPointerException()
            Log.e(TAG, "createNewJamMeet: Couldn't create key for new JamMeet", exception)
            continuation.resumeWith(Result.failure(exception))
            return@suspendCoroutine
        }
        val jamMeetKeyValuePairs = with (jamMeet.copy(id = meetId)) {
            mapOf(
                "id" to id,
                "latitude" to latitude,
                "longitude" to longitude,
                "utcTimeStamp" to utcTimeStamp
            )
        }
        ref.updateChildren(jamMeetKeyValuePairs){ error, _ ->
            if (error == null) {
                continuation.resumeWith(Result.success(Unit))
            } else {
                continuation.resumeWith(Result.failure(error.toException()))
            }
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
