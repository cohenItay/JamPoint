package com.itaycohen.jampoint.ui.jam_team

import android.content.Context
import android.view.View
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.savedstate.SavedStateRegistryOwner
import com.google.android.material.button.MaterialButton
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.data.models.Jam
import com.itaycohen.jampoint.data.models.local.*
import com.itaycohen.jampoint.data.repositories.JamPlacesRepository
import com.itaycohen.jampoint.data.repositories.UserRepository
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeParseException

class JamTeamViewModel(
    appContext: Context,
    handle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val jamPlacesRepository: JamPlacesRepository,
) : ViewModel() {

    val teamItemsLiveData: LiveData<List<TeamItemModel>> = MutableLiveData(listOf())
    val isManagerLiveData: LiveData<Boolean> = MutableLiveData(false)
    val isInEditModeLiveData: LiveData<Boolean> = MutableLiveData(false)

    private var isMembershipPending: Boolean = false
    private var jamPointId: String? = null

    fun updateJamPlaceId(jamPlaceId: String?) {
        teamItemsLiveData as MutableLiveData
        val jamsMap = jamPlacesRepository.jamPlacesLiveData.value ?: return
        val jamPlace = jamsMap[jamPlaceId]
        teamItemsLiveData.value = transformToTeamItems(jamPlace) ?: listOf()
        val user = userRepository.userLiveData.value
        (isManagerLiveData as MutableLiveData).value = user != null &&
                (jamPlace?.groupManagers?.containsKey(user.id) ?: false)
    }

    fun onParticipateRequestClick(navController: NavController, jamMeetIndex: Int?) {
        val id = jamPointId ?: return
        val user = userRepository.userLiveData.value ?: return
        if (isMembershipPending) {
            check (jamMeetIndex == null) {
                "Cannot join to future meeting while also pending to join team"
            }
            viewModelScope.launch {
                jamPlacesRepository.updateMembership(user, id, false)
            }
        } else {
            val teamFutureModel = teamItemsLiveData.value!!
                .filterIsInstance(TeamItemFutureMeetings::class.java)
                .firstOrNull()
            val jamMeet = jamMeetIndex?.let { teamFutureModel?.futureMeetings?.get(it) }
            if (jamMeet != null) {
                viewModelScope.launch {
                    val isPendingForMeet = teamFutureModel?.isPendingForList?.get(jamMeetIndex) ?: false
                    if (isPendingForMeet) {
                        jamPlacesRepository.updateMeetingParticipationFor(user, id, jamMeet, false)
                    } else {
                        val action = JamTeamFragmentDirections
                            .actionJamTeamDialogFragmentToJoinTeamDialogFragment(jamMeet, id)
                        navController.navigate(action)
                    }
                }
            } else {
                val action = JamTeamFragmentDirections
                    .actionJamTeamDialogFragmentToJoinTeamDialogFragment(null, id)
                navController.navigate(action)
            }
        }
    }

    fun onEditModeClick(v: View) {
        isInEditModeLiveData as MutableLiveData
        isInEditModeLiveData.value = !isInEditModeLiveData.value!!
    }



    private fun transformToTeamItems(jam: Jam?) : List<TeamItemModel> {
        teamItemsLiveData as MutableLiveData
        if (jam == null) {
            teamItemsLiveData.value = listOf()
            return listOf()
        }
        jamPointId = jam.jampPointId
        isMembershipPending = userRepository.userLiveData.value?.let { user ->
            jam.pendingMembers?.containsKey(user.id)
        } ?: false
        val items = mutableListOf<TeamItemModel>()
        jam.jamPlaceNickname?.also { items.add(TeamItemName(it, jam.isLive == true)) }
        jam.members?.also {
            if (it.isNotEmpty())
                items.add(TeamItemMembers(it))
        }
        jam.searchedInstruments?.also {
            if (it.isNotEmpty())
                items.add(TeamItemSearchedInstruments(it, isMembershipPending))
        }
        jam.jamMeetings?.also { jamMeetings ->
            val (futureMeetings, pastMeetings) = jamMeetings.partition { jamMeet ->
                try {
                    Instant.parse(jamMeet.utcTimeStamp).isAfter(Instant.now())
                } catch (e: DateTimeParseException) {
                    false
                }
            }
            if (futureMeetings.isNotEmpty()) {
                val list = if (futureMeetings.lastIndex > 7) futureMeetings.subList(0, 7) else futureMeetings
                val isPendingForList = list.map { jamMeet ->
                    val pendingMeetId = jamMeet.utcTimeStamp?.split(".")?.get(0) ?: return@map false
                    userRepository.userLiveData.value?.let { user ->
                        val pendingMeet = jam.pendingJoinMeeting?.get(pendingMeetId) ?: return@let false
                        pendingMeet[user.id] == true
                    } ?: false
                }
                items.add(TeamItemFutureMeetings(list, isPendingForList, isMembershipPending))
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
