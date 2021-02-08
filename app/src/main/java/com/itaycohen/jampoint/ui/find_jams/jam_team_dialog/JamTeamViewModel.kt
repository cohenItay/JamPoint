package com.itaycohen.jampoint.ui.find_jams.jam_team_dialog

import android.content.Context
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.savedstate.SavedStateRegistryOwner
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
    jamPlaceKey: String
) : ViewModel() {

    val teamItemsLiveData: LiveData<List<TeamItemModel>> = jamPlacesRepository.jamPlacesLiveData.map {
        it[jamPlaceKey]?.let { jam ->
            transformToTeamItems(jam)
        } ?: listOf()
    }

    private var isMembershipPending: Boolean = false
    private var jamPointId: String? = null

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
                        val action = JamTeamDialogFragmentDirections
                            .actionJamTeamDialogFragmentToJoinTeamDialogFragment(jamMeet, id)
                        navController.navigate(action)
                    }
                }
            } else {
                val action = JamTeamDialogFragmentDirections
                    .actionJamTeamDialogFragmentToJoinTeamDialogFragment(null, id)
                navController.navigate(action)
            }
        }
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
        private val jamPlaceKey: String
    ) : AbstractSavedStateViewModelFactory(regOwner, null) {
        override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
            return JamTeamViewModel(
                appContext,
                handle,
                AppServiceLocator.userRepository,
                AppServiceLocator.jamPlacesRepository,
                jamPlaceKey
            ) as T
        }
    }

    companion object {
        private val TAG = JamTeamViewModel::class.java.simpleName
    }
}
