package com.itaycohen.jampoint.ui.sign_up

import android.content.Context
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.data.repositories.UserRepository

class LoginViewModel(
    appContext: Context,
    handle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {

    fun createFirebaseLoginContract() = userRepository.createFirebaseLoginContract()

    class Factory(
        owner: SavedStateRegistryOwner,
        private val appContext: Context
    ): AbstractSavedStateViewModelFactory(owner, null) {
        override fun <T : ViewModel?> create(
            key: String,
            modelClass: Class<T>,
            handle: SavedStateHandle
        ): T {
            return LoginViewModel(
                appContext,
                handle,
                AppServiceLocator.userRepository
            ) as T
        }

    }
}
