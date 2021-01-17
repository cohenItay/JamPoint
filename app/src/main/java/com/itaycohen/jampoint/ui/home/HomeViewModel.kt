package com.itaycohen.jampoint.ui.home

import android.content.Context
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.utils.GsonContainer
import com.itaycohen.jampoint.utils.SharedPrefsHelper

class HomeViewModel(
    private val appContext: Context,
    private val prefsHelper: SharedPrefsHelper,
    handle: SavedStateHandle
) : ViewModel() {

    val isInFirstEntranceSession: LiveData<Boolean>
    val placeTextLiveData: LiveData<String?> = MutableLiveData(null)
    val placeErrorLiveData : LiveData<String?> = MutableLiveData(null)

    init {
        val isFirst = prefsHelper.getValue(IS_FIRST_ENTRANCE_KEY, true)
        isInFirstEntranceSession = MutableLiveData(isFirst)
    }

    fun endFirstEntranceSession() {
        prefsHelper.saveValue(IS_FIRST_ENTRANCE_KEY, false)
        (isInFirstEntranceSession as MutableLiveData).value = false
    }

    fun initPlacesFragmentConfiguration(placesFragment: AutocompleteSupportFragment) = with (placesFragment) {
        setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))
        setCountries("IL")
        setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                (placeTextLiveData as MutableLiveData). value = place.name
            }
            override fun onError(status: Status) {
                (placeErrorLiveData as MutableLiveData).value = if (status.isSuccess || status.isCanceled)
                    null
                else
                    appContext.getString(R.string.problem_with_place)
            }
        })
    }

    class Factory(
        regOwner: SavedStateRegistryOwner,
        private val appContext: Context
    ) : AbstractSavedStateViewModelFactory(regOwner, null) {
        private val prefsHelperFactory = SharedPrefsHelper.Factory(appContext, GsonContainer.instance)
        override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
            return HomeViewModel(appContext, prefsHelperFactory.create("HomePrefs"), handle) as T
        }
    }

    companion object {
        private const val IS_FIRST_ENTRANCE_KEY = "kjdang"
    }
}