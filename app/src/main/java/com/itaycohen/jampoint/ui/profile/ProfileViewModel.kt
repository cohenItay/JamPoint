package com.itaycohen.jampoint.ui.profile

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.*
import androidx.navigation.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.QueryState
import com.itaycohen.jampoint.data.repositories.UserRepository
import com.itaycohen.jampoint.utils.PickImageContract
import kotlinx.coroutines.launch
import java.lang.Exception

class ProfileViewModel(
    private val appContext: Context,
    handle: SavedStateHandle,
    private val userRepository: UserRepository
) : ViewModel() {

    val userLiveData = userRepository.userLiveData
    val instrumentQueryState: LiveData<QueryState> = MutableLiveData(QueryState.Idle)
    private lateinit var pickImageLauncher: ActivityResultLauncher<Unit>

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

    fun setActivityResultLaunchers(activityResultRegistry: ActivityResultRegistry, owner: LifecycleOwner) {
        pickImageLauncher = activityResultRegistry.register("", owner, PickImageContract(), userRepository::updateCharacterizesImage)
    }

    fun pickImage() {
        pickImageLauncher.launch(Unit)
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
