package com.itaycohen.jampoint.ui.home

import android.content.Context
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.itaycohen.jampoint.utils.GsonContainer
import com.itaycohen.jampoint.utils.SharedPrefsHelper

class HomeViewModel(
    private val prefsHelper: SharedPrefsHelper,
    handle: SavedStateHandle
) : ViewModel() {

    val isInFirstEntranceSession: LiveData<Boolean>

    init {
        val isFirst = prefsHelper.getValue(IS_FIRST_ENTRANCE_KEY, true)
        isInFirstEntranceSession = MutableLiveData(isFirst)
    }

    fun endFirstEntranceSession() {
        prefsHelper.saveValue(IS_FIRST_ENTRANCE_KEY, false)
    }

    class Factory(
        regOwner: SavedStateRegistryOwner,
        private val appContext: Context
    ) : AbstractSavedStateViewModelFactory(regOwner, null) {
        private val prefsHelperFactory = SharedPrefsHelper.Factory(appContext, GsonContainer.instance)
        override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
            return HomeViewModel(prefsHelperFactory.create("HomePrefs"), handle) as T
        }
    }

    companion object {
        private const val IS_FIRST_ENTRANCE_KEY = "kjdang"
    }
}