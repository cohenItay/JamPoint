package com.itaycohen.jampoint.ui.jam_team.join_request

import android.content.Context
import android.util.Log
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.savedstate.SavedStateRegistryOwner
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.data.models.JamMeet
import com.itaycohen.jampoint.data.repositories.JamPlacesRepository
import com.itaycohen.jampoint.data.repositories.UserRepository
import kotlinx.coroutines.launch
import java.lang.Exception

class JoinTeamViewModel(
    appContext: Context,
    handle: SavedStateHandle,
    userRepository: UserRepository,
    private val jamPlacesRepository: JamPlacesRepository
) : ViewModel() {

    val userLiveData = userRepository.userLiveData


    fun requestToJoin(jamPointId: String, jamMeeting: JamMeet? = null) {
        val user = userLiveData.value ?: return
        viewModelScope.launch {
            try {
                if (jamMeeting == null)
                    jamPlacesRepository.jamPointMembershipRequest(user, jamPointId, true)
                else
                    jamPlacesRepository.updateMeetingParticipationFor(user, jamPointId, jamMeeting, true)
            } catch (e: Exception) {
                Log.e(TAG, "requestToJoin: ", e)
            }
        }
    }

    class Factory(
        owner: SavedStateRegistryOwner,
        private val appContext: Context
    ): AbstractSavedStateViewModelFactory(owner, null) {
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            @Suppress("UNCHECKED_CAST")
            return JoinTeamViewModel(
                appContext,
                handle,
                AppServiceLocator.userRepository,
                AppServiceLocator.jamPlacesRepository
            ) as T
        }

    }

    companion object {
        private val TAG = JoinTeamViewModel::class.simpleName
    }
}
