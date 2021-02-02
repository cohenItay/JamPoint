package com.itaycohen.jampoint.ui.home.jam_team_dialog.join_request

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.data.repositories.UserRepository
import com.itaycohen.jampoint.ui.sign_up.LoginDialogFragment

class JoinTeamViewModel(
    appContext: Context,
    handle: SavedStateHandle,
    userRepository: UserRepository
) : ViewModel() {

    val userLiveData = userRepository.userLiveData

    class Factory(
        owner: SavedStateRegistryOwner,
        private val appContext: Context
    ): AbstractSavedStateViewModelFactory(owner, null) {
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            return JoinTeamViewModel(
                appContext,
                handle,
                AppServiceLocator.userRepository
            ) as T
        }

    }
}
