package com.itaycohen.jampoint.ui.home.jam_team_dialog

import android.content.Context
import android.view.View
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.data.models.Jam
import com.itaycohen.jampoint.data.models.local.*
import com.itaycohen.jampoint.data.repositories.JamPlacesRepository
import com.itaycohen.jampoint.data.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeParseException

class JamTeamViewModel(
    appContext: Context,
    handle: SavedStateHandle,
    userRepository: UserRepository,
    private val jamPlacesRepository: JamPlacesRepository,
    jamPlaceKey: String
) : ViewModel() {

    val teamItemsLiveData: LiveData<List<TeamItemModel>> = MutableLiveData(listOf())

    init {
        jamPlacesRepository.jamPlacesLiveData.value?.get(jamPlaceKey)?.also { jam ->
            transformToTeamItems(jam)
        }
    }

    fun onAskToJoin(v: View) {
        //TODO
    }

    private fun transformToTeamItems(jam: Jam?) {
        teamItemsLiveData as MutableLiveData
        if (jam == null) {
            teamItemsLiveData.value = listOf()
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            val items = mutableListOf<TeamItemModel>()
            jam.jamPlaceNickname?.also { items.add(TeamItemName(it, jam.isLive == true)) }
            jam.membersIds?.also {
                jamPlacesRepository.getMusiciansInfo(it)?.also { members ->
                    items.add(TeamItemMembers(members.toList()))
                }
            }
            jam.searchedInstruments?.also {
                if (it.isNotEmpty())
                    items.add(TeamItemSearchedInstruments(it))
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
                    items.add(TeamItemFutureMeetings(futureMeetings, jam.searchedInstruments))
                }
                if (pastMeetings.isNotEmpty()) {
                    items.add(TeamItemPastMeetings(futureMeetings))
                }
            }
            teamItemsLiveData.postValue(items)
        }
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
}
