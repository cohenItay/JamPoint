package com.itaycohen.jampoint.ui.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.ui.permissions.NoPermissionModel
import com.itaycohen.jampoint.ui.permissions.RationalDialogFragment
import com.itaycohen.jampoint.ui.permissions.RationalModel
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

    fun createLocationActivityResultCallback(navControllerCallback: ()->NavController) = { isGranted: Boolean ->
        if (isGranted) {
            trackUserLocation()
        } else {
            explainNoPermissionConsequence(navControllerCallback())
        }
    }

    fun trackUserOrlaunchLocationPermissionLogic(fragment: Fragment, rpl: ActivityResultLauncher<String>) {
        val permissionState = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            trackUserLocation()
        } else {
            val useRational = ActivityCompat.shouldShowRequestPermissionRationale(fragment.requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
            if (useRational) {
                val rationalModel = RationalModel(R.string.location_rational_message)
                val action = HomeFragmentDirections.actionGlobalRationalDialog(rationalModel)
                fragment.setFragmentResultListener(RationalDialogFragment.REQUEST_RESULT_KEY, createRationalDialogCallback(fragment, rpl))
                fragment.findNavController().navigate(action)
            } else {
                // If the user decided not be asked again for location permission it wouldn't do a thing:
                rpl.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }

    private fun createRationalDialogCallback(fragment: Fragment, rpl: ActivityResultLauncher<String>) = CB@{ reqKey: String, bundle: Bundle ->
        if (reqKey != RationalDialogFragment.REQUEST_RESULT_KEY)
            return@CB

        val isAccepted = bundle.getInt(
            RationalDialogFragment.RATIONAL_KEY,
            RationalDialogFragment.RATIONAL_DISAGREE
        ) == RationalDialogFragment.RATIONAL_AGREE
        if (isAccepted) {
            rpl.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        } else {
            explainNoPermissionConsequence(fragment.findNavController())
        }
    }

    private fun explainNoPermissionConsequence(navController: NavController) {
        val noPermissionModel = NoPermissionModel(R.string.no_location_permission_message)
        val action = HomeFragmentDirections.actionGlobalNoPermissionDialog(noPermissionModel)
        navController.navigate(action)
    }

    private fun trackUserLocation() {
        //TODO: implement
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