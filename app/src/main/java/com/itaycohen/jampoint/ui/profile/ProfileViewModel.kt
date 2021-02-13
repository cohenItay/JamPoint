package com.itaycohen.jampoint.ui.profile

import android.content.Context
import android.view.View
import androidx.lifecycle.*
import androidx.navigation.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.QueryState
import com.itaycohen.jampoint.data.repositories.UserRepository
import kotlinx.coroutines.launch
import java.lang.Exception

class ProfileViewModel(
    private val appContext: Context,
    handle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {

    val userLiveData = userRepository.userLiveData
    val instrumentQueryState: LiveData<QueryState> = MutableLiveData(QueryState.Idle)

    fun onSignOut(v: View) {
        userRepository.doLogout()
    }

    fun onSignIn(v: View) {
        val action = ProfileFragmentDirections.actionGlobalLoginFragment()
        v.findNavController().navigate(action)
    }

    fun updateUserInstrument(instrument: String) {
        instrumentQueryState as MutableLiveData
        viewModelScope.launch {
            try {
                instrumentQueryState.value = QueryState.Running
                userRepository.updateUserInstrument(instrument)
                instrumentQueryState.value = QueryState.Success
            } catch (e : Exception) {
                instrumentQueryState.value = QueryState.Failure(appContext.getString(R.string.problem_update_instrument))
            }
        }
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
