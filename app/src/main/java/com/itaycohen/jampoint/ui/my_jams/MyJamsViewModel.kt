package com.itaycohen.jampoint.ui.my_jams

import android.content.Context
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import androidx.navigation.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DatabaseException
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.Jam
import com.itaycohen.jampoint.data.models.User
import com.itaycohen.jampoint.data.models.QueryState
import com.itaycohen.jampoint.data.repositories.JamPlacesRepository
import com.itaycohen.jampoint.data.repositories.UserRepository
import kotlinx.coroutines.launch

class MyJamsViewModel(
    private val appContext: Context,
    handle: SavedStateHandle,
    private val userRepository: UserRepository,
    private val jamsRepository: JamPlacesRepository
) : ViewModel() {

    val userLiveData = userRepository.userLiveData
    val selfJamsLiveData: LiveData<Map<String, Jam>?> = MutableLiveData()
    val queryStateLiveData: LiveData<QueryState> = MutableLiveData(QueryState.Idle)

    fun fetchSelfJams() {
        val userId = userRepository.userLiveData.value?.id ?: return
        viewModelScope.launch {
            queryStateLiveData as MutableLiveData
            try {
                queryStateLiveData.value = QueryState.Running
                val data = jamsRepository.fetchSelfJams(userId)
                (selfJamsLiveData as MutableLiveData).value = data
                queryStateLiveData.value = QueryState.Success
            } catch (e: DatabaseException) {
                Log.e(TAG, "fetchSelfJams: failure", e)
                queryStateLiveData.value = QueryState.Failure(appContext.getString(R.string.database_fetch_error))
            }
        }
    }

    fun createNewJamPoint(nickName: String, onResult: (newJamPoint: Jam?) -> Unit) {
        val user = userRepository.userLiveData.value ?: return
        viewModelScope.launch {
            try {
                val newJamPoint = jamsRepository.createNewJamPoint(nickName, user)
                fetchSelfJams()
                onResult(newJamPoint)
            } catch (e: DatabaseException) {
                Log.e(TAG, "createNewJamPoint: ", e)
                onResult(null)
            }
        }
    }

    fun onJamPlaceClick(v: View, jamPlaceId: String) {
        val action = MyJamsFragmentDirections.actionMyJamsFragmentToJamTeamDialogFragment(jamPlaceId)
        v.findNavController().navigate(action)
    }

    fun onLiveClick(v: MaterialButton, jamPlaceId: String) {
        v.isEnabled = false
        viewModelScope.launch {
            try {
                jamsRepository.updateIsLive(jamPlaceId, !v.isChecked)
                v.isChecked = !v.isChecked
            } catch (e: DatabaseException) {
                Log.e(TAG, "onLiveClick: ", e)
            }
            v.isEnabled = true
        }
    }



    class Factory(
        regOwner: SavedStateRegistryOwner,
        private val appContext: Context,
    ) : AbstractSavedStateViewModelFactory(regOwner, null) {
        override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
            return MyJamsViewModel(
                appContext,
                handle,
                AppServiceLocator.userRepository,
                AppServiceLocator.jamPlacesRepository
            ) as T
        }
    }

    companion object {
        private val TAG = MyJamsViewModel::class.simpleName
    }
}
