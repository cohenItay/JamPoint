package com.itaycohen.jampoint.ui.jam_team

import android.content.Context
import android.location.Location
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import androidx.savedstate.SavedStateRegistryOwner
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DatabaseException
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.Jam
import com.itaycohen.jampoint.data.models.JamMeet
import com.itaycohen.jampoint.data.models.User
import com.itaycohen.jampoint.data.models.local.*
import com.itaycohen.jampoint.data.repositories.JamPlacesRepository
import com.itaycohen.jampoint.data.repositories.UserRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeParseException

class JamTeamViewModel(
    private val appContext: Context,
    handle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val jamPlacesRepository: JamPlacesRepository,
) : ViewModel() {

    val userLiveData = userRepository.userLiveData
    val teamItemsLiveData: LiveData<List<TeamItemModel>> = MutableLiveData(listOf())
    val isManagerLiveData: LiveData<Boolean> = MutableLiveData(false)
    val isInEditModeLiveData: LiveData<Boolean> = MutableLiveData(false)

    private var membershipState: MembershipState = MembershipState.No
    private var jamPointId: String? = null

    fun updateJamPlaceId(jamPlaceId: String) {
        viewModelScope.launch {
            teamItemsLiveData as MutableLiveData
            val jamPlace = jamPlacesRepository.fetchJam(jamPlaceId)
            val user = userRepository.userLiveData.value
            (isManagerLiveData as MutableLiveData).value = user != null &&
                    (jamPlace?.groupManagers?.containsKey(user.id) ?: false)
            teamItemsLiveData.value = transformToTeamItems(jamPlace)
        }
    }

    fun onParticipateRequestClick(navController: NavController, jamMeetIndex: Int?) {
        val pointId = jamPointId ?: return
        val user = userRepository.userLiveData.value
        val teamFutureModel = teamItemsLiveData.value!!
            .filterIsInstance(TeamItemFutureMeetings::class.java)
            .firstOrNull()
        val jamMeet = jamMeetIndex?.let { teamFutureModel?.futureMeetings?.get(it) }

        if (jamMeetIndex == null) {
            // Team membership related:
            when (membershipState) {
                MembershipState.Manager -> return
                MembershipState.No,
                MembershipState.Yes -> {
                    val wantToJoin = membershipState == MembershipState.No
                    if (wantToJoin) {
                        val action = JamTeamFragmentDirections.actionGlobalJoinTeamDialogFragment(null, pointId)
                        navController.navigate(action)
                    } else {
                        user ?: return
                        viewModelScope.launch {
                            try {
                                jamPlacesRepository.jamPointMembershipRequest(user, pointId, wantToJoin)
                                updateJamPlaceId(pointId)
                            } catch (e: DatabaseException) {
                                Log.e(TAG, "onParticipateRequestClick: Remove user membership", e)
                            }
                        }
                    }
                }
                MembershipState.Pending -> {
                    // User wants to remove pending:
                    user ?: return
                    viewModelScope.launch {
                        try {
                            jamPlacesRepository.jamPointMembershipRequest(user, pointId, false)
                            updateJamPlaceId(pointId)
                        } catch (e: DatabaseException) {
                            Log.e(TAG, "onParticipateRequestClick: Remove user pending membership", e)
                        }
                    }
                }
            }
        } else {
            // Future meet related:
            jamMeet ?: return
            val isPendingForMeet = teamFutureModel?.futureMeetingsSelfPendingList?.get(jamMeetIndex) ?: false
            val wantToParticiapte = !isPendingForMeet
            if (wantToParticiapte) {
                val action = JamTeamFragmentDirections.actionGlobalJoinTeamDialogFragment(jamMeet, pointId)
                navController.navigate(action)
            } else {
                user ?: return
                viewModelScope.launch {
                    try {
                        jamPlacesRepository.updateMeetingParticipateRequestFor(user, pointId, jamMeet, wantToParticiapte)
                        updateJamPlaceId(pointId)
                    } catch (e: DatabaseException) {
                        Log.e(TAG, "onParticipateRequestClick: Remove user from future meet", e)
                    }
                }
            }
        }
    }

    fun onEditModeClick(v: View) {
        isInEditModeLiveData as MutableLiveData
        isInEditModeLiveData.value = !isInEditModeLiveData.value!!
        val items = teamItemsLiveData.value?.toMutableList() ?: return
        if (membershipState == MembershipState.Manager) {
            if (isInEditModeLiveData.value!!) {
                var index = items.indexOfFirst {
                    it is TeamItemSearchedInstruments
                }
                if (index == -1) {
                    index = items.indexOfFirst {
                        it is TeamItemMembers
                    }
                }
                if (index == -1)
                    return
                items.add(index+1, TeamItemCreateJamMeet(appContext.getString(R.string.create_meeting)))
            } else {
                items.removeIf { it is TeamItemCreateJamMeet }
            }
            teamItemsLiveData as MutableLiveData
            teamItemsLiveData.value = items
        }
    }

    fun onLiveBtnClick(v: MaterialButton) {
        val id = jamPointId ?: return
        v.isEnabled = false
        viewModelScope.launch {
            try {
                jamPlacesRepository.updateIsLive(id, v.isChecked)
                updateJamPlaceId(id)
            } catch (e: DatabaseException) {
                Log.e(TAG, "onLiveBtnClick: ", e)
            }
            v.isEnabled = true
        }
    }

    fun updateJamTeamRequiredUsers(newInstruments: List<String>) {
        val id = jamPointId ?: return
        viewModelScope.launch {
            try {
                jamPlacesRepository.updateJamTeamRequiredUsers(id, newInstruments)
                updateJamPlaceId(id)
            } catch (e: DatabaseException) {
                Log.e(TAG, "updateJamTeamRequiredUsers: ", e)
            }
        }
    }

    fun updateMembershipConfirmation(user: User, confirmed: Boolean) {
        val id = jamPointId ?: return
        viewModelScope.launch {
            try {
                jamPlacesRepository.jamPointMembershipAnswer(user, id, confirmed)
                updateJamPlaceId(id)
            } catch (e: DatabaseException) {
                Log.e(TAG, "updateJamTeamRequiredUsers: ", e)
            }
        }
    }

    fun updateJoinMeetingConfirmation(futureMeet: JamMeet, user: User, isConfirmed: Boolean) {
        val id = jamPointId ?: return
        viewModelScope.launch {
            try {
                jamPlacesRepository.jamPointJoinMeetingAnswer(futureMeet, user, id, isConfirmed)
                updateJamPlaceId(id)
            } catch (e: DatabaseException) {
                Log.e(TAG, "updateJoinMeetingConfirmation: ", e)
            }
        }
    }

    fun removeUserFromMeeting(futureMeet: JamMeet, user: User) {
        val id = jamPointId ?: return
        viewModelScope.launch {
            try {
                jamPlacesRepository.removeUserFromMeeting(futureMeet, user, id)
                updateJamPlaceId(id)
            } catch (e: DatabaseException) {
                Log.e(TAG, "removeUserFromMeeting: ", e)
            }
        }
    }

    fun updateMeetingTime(futureMeet: JamMeet, utcTimeStamp: String) {
        val id = jamPointId ?: return
        viewModelScope.launch {
            try {
                jamPlacesRepository.updateMeetingTime(futureMeet, id, utcTimeStamp)
                updateJamPlaceId(id)
            } catch (e: DatabaseException) {
                Log.e(TAG, "updateMeetingTime: ", e)
            }
        }
    }


    fun updateMeetingPlace(futureMeet: JamMeet, location: Location) {
        val id = jamPointId ?: return
        viewModelScope.launch {
            try {
                jamPlacesRepository.updateMeetingPlace(futureMeet, id, location)
                updateJamPlaceId(id)
            } catch (e: DatabaseException) {
                Log.e(TAG, "updateMeetingPlace: ", e)
            }
        }
    }

    fun createNewJamMeet(jamMeet: JamMeet) {
        val pointId = jamPointId ?: return
        viewModelScope.launch {
            try {
                jamPlacesRepository.createNewJamMeet(pointId, jamMeet)
                updateJamPlaceId(pointId)
            } catch (e: DatabaseException) {
                Log.e(TAG, "updateMeetingPlace: ", e)
            }
        }
    }




    private fun transformToTeamItems(jam: Jam?) : List<TeamItemModel> {
        teamItemsLiveData as MutableLiveData
        if (jam == null) {
            teamItemsLiveData.value = listOf()
            return listOf()
        }
        jamPointId = jam.jamPointId
        membershipState = userRepository.userLiveData.value?.let { user ->
            jam.getMembershipStateFor(user.id)
        } ?: MembershipState.No
        val items = mutableListOf<TeamItemModel>()
        jam.jamPlaceNickname?.also {
            items.add(TeamItemName(
                it,
                jam.isLive == true,
                isManagerLiveData.value!!)
            )
        }
        jam.members?.values?.toList()?.also {
            if (it.isNotEmpty())
                items.add(TeamItemMembers(it))
        }
        jam.searchedInstruments?.also {
            if (it.isNotEmpty()) {
                items.add(TeamItemSearchedInstruments(
                    it,
                    jam.pendingMembers?.values?.toList() ?: listOf(),
                    membershipState
                ))
            }
        }
        jam.jamMeetings?.also { jamMeetings ->
            val (futureMeetings, pastMeetings) = jamMeetings.values.partition { jamMeet ->
                try {
                    Instant.parse(jamMeet.utcTimeStamp).isAfter(Instant.now())
                } catch (e: DateTimeParseException) {
                    false
                }
            }
            if (futureMeetings.isNotEmpty()) {
                val list = if (futureMeetings.lastIndex > 7) futureMeetings.subList(0, 7) else futureMeetings
                val futureMeetingsSelfPendingList = list.map { jamMeet ->
                    userLiveData.value?.let { user ->
                        jamMeet.pendingMembers?.get(user.id) != null
                    } ?: false
                }
                items.add(TeamItemFutureMeetings(
                    list,
                    futureMeetingsSelfPendingList,
                    membershipState
                ))
            }
            if (pastMeetings.isNotEmpty()) {
                val list = if (pastMeetings.lastIndex > 7) pastMeetings.subList(0, 7) else pastMeetings
                items.add(TeamItemPastMeetings(list))
            }
        }
        return items
    }


    class Factory(
        regOwner: SavedStateRegistryOwner,
        private val appContext: Context,
    ) : AbstractSavedStateViewModelFactory(regOwner, null) {
        override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
            return JamTeamViewModel(
                appContext,
                handle,
                AppServiceLocator.userRepository,
                AppServiceLocator.jamPlacesRepository
            ) as T
        }
    }

    companion object {
        private val TAG = JamTeamViewModel::class.java.simpleName
    }
}
