package com.itaycohen.jampoint.ui.profile

import android.content.Context
import android.view.View
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.data.repositories.UserRepository

class ProfileViewModel(
    appContext: Context,
    handle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {

    val userLiveData = userRepository.userLiveData

    fun onSignOut(v: View) {
        userRepository.doLogout()
    }

    fun onSignIn(v: View) {
        val action = ProfileFragmentDirections.actionProfileFragmentToLoginDialogFragment()
        v.findNavController().navigate(action)
    }


    class Factory(
        regOwner: SavedStateRegistryOwner,
        private val appContext: Context,
    ) : AbstractSavedStateViewModelFactory(regOwner, null) {
        override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
            return ProfileViewModel(
                appContext,
                handle,
                AppServiceLocator.userRepository,
            ) as T
        }
    }
}
