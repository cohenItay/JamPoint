package com.itaycohen.jampoint.ui.my_jams

import android.content.Context
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.data.repositories.UserRepository

class MyJamsViewModel(
    appContext: Context,
    handle: SavedStateHandle,
    userRepository: UserRepository
) : ViewModel() {




    class Factory(
        regOwner: SavedStateRegistryOwner,
        private val appContext: Context,
    ) : AbstractSavedStateViewModelFactory(regOwner, null) {
        override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
            return MyJamsViewModel(
                appContext,
                handle,
                AppServiceLocator.userRepository,
            ) as T
        }
    }
}
