package com.itaycohen.jampoint.ui.my_jams

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseException
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.Jam
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
