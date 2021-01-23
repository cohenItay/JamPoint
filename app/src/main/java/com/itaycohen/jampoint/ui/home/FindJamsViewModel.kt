package com.itaycohen.jampoint.ui.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.*
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.savedstate.SavedStateRegistryOwner
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.repositories.LocationRepository
import com.itaycohen.jampoint.services.LocationService
import com.itaycohen.jampoint.ui.permissions.NoPermissionModel
import com.itaycohen.jampoint.ui.permissions.RationalDialogFragment
import com.itaycohen.jampoint.ui.permissions.RationalModel
import com.itaycohen.jampoint.utils.GsonContainer
import com.itaycohen.jampoint.utils.SharedPrefsHelper

class FindJamsViewModel(
    private val appContext: Context,
    private val prefsHelper: SharedPrefsHelper,
    handle: SavedStateHandle,
    private val locationRepository: LocationRepository
) : ViewModel() {

    val isInFirstEntranceSession: LiveData<Boolean>
    val placeTextLiveData: LiveData<String?> = MutableLiveData(null)
    val placeErrorLiveData : LiveData<String?> = MutableLiveData(null)
    val locationLiveData = locationRepository.locationLiveData
    val locationStateLiveData = locationRepository.locationStateLiveData

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

    fun createLocationActivityResultCallback(fragmentCallback: ()->Fragment) = { isGranted: Boolean ->
        if (isGranted) {
            validateClientSettingsForLocation(fragmentCallback())
        } else {
            explainNoPermissionConsequence(fragmentCallback().findNavController())
        }
    }

    fun locateSelf(fragment: Fragment, rpl: ActivityResultLauncher<String>) {
        val permissionState = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            validateClientSettingsForLocation(fragment)
        } else {
            val useRational = ActivityCompat.shouldShowRequestPermissionRationale(fragment.requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
            if (useRational) {
                val rationalModel = RationalModel(R.string.location_rational_message)
                val action = FindJamsFragmentDirections.actionGlobalRationalDialog(rationalModel)
                fragment.setFragmentResultListener(RationalDialogFragment.REQUEST_RESULT_KEY, createRationalDialogCallback(fragment, rpl))
                fragment.findNavController().navigate(action)
            } else {
                // If the user decided not be asked again for location permission it wouldn't do a thing:
                rpl.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    fun handleLocationSettingsResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK)
            startLocationService()
    }

    private fun createRationalDialogCallback(fragment: Fragment, rpl: ActivityResultLauncher<String>) = CB@{ reqKey: String, bundle: Bundle ->
        if (reqKey != RationalDialogFragment.REQUEST_RESULT_KEY)
            return@CB

        val isAccepted = bundle.getInt(
            RationalDialogFragment.RATIONAL_KEY,
            RationalDialogFragment.RATIONAL_DISAGREE
        ) == RationalDialogFragment.RATIONAL_AGREE
        if (isAccepted) {
            rpl.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            explainNoPermissionConsequence(fragment.findNavController())
        }
    }

    private fun explainNoPermissionConsequence(navController: NavController) {
        val noPermissionModel = NoPermissionModel(R.string.no_location_permission_message)
        val action = FindJamsFragmentDirections.actionGlobalNoPermissionDialog(noPermissionModel)
        navController.navigate(action)
    }

    private fun validateClientSettingsForLocation(fragment: Fragment) {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(createLocationRequest())

        val client: SettingsClient = LocationServices.getSettingsClient(appContext)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            startLocationService()
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult()
                    exception.startResolutionForResult(fragment.requireActivity(), REQUEST_CHECK_LOCATOIN_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun createLocationRequest() = LocationRequest.create().apply {
        interval = 10_000
        priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
    }

    private fun startLocationService() {
        val intent = Intent(appContext, LocationService::class.java).apply {
            putExtra(LocationService.LOCATION_REQUEST_PARAMS, createLocationRequest())
        }
        ContextCompat.startForegroundService(appContext, intent)
    }

    class Factory(
        regOwner: SavedStateRegistryOwner,
        private val appContext: Context
    ) : AbstractSavedStateViewModelFactory(regOwner, null) {
        private val prefsHelperFactory = SharedPrefsHelper.Factory(appContext, GsonContainer.instance)
        override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
            return FindJamsViewModel(appContext, prefsHelperFactory.create("HomePrefs"), handle, AppServiceLocator.locationRepository) as T
        }
    }

    companion object {
        private const val IS_FIRST_ENTRANCE_KEY = "kjdang"
        const val REQUEST_CHECK_LOCATOIN_SETTINGS = 31
    }
}