package com.itaycohen.jampoint.ui.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.itaycohen.jampoint.AppServiceLocator
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.Jam
import com.itaycohen.jampoint.data.models.QueryState
import com.itaycohen.jampoint.data.repositories.LocationRepository
import com.itaycohen.jampoint.data.repositories.JamPlacesRepository
import com.itaycohen.jampoint.data.repositories.UserRepository
import com.itaycohen.jampoint.ui.permissions.NoPermissionModel
import com.itaycohen.jampoint.ui.permissions.RationalDialogFragment
import com.itaycohen.jampoint.ui.permissions.RationalModel
import com.itaycohen.jampoint.utils.GsonContainer
import com.itaycohen.jampoint.utils.SharedPrefsHelper
import com.itaycohen.jampoint.utils.toLocation
import kotlinx.coroutines.launch

class FindJamsViewModel(
    private val appContext: Context,
    private val prefsHelper: SharedPrefsHelper,
    handle: SavedStateHandle,
    private val locationRepository: LocationRepository,
    private val jamPlacesRepository: JamPlacesRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val isInFirstEntranceSession: LiveData<Boolean>
    val placeLiveData: LiveData<Place?> = MutableLiveData(null)
    val placeErrorLiveData : LiveData<String?> = MutableLiveData(null)
    val userLiveData = userRepository.userLiveData
    val serviceStateLiveData = locationRepository.serviceStateLiveData
    val jamPlacesLiveData: LiveData<Map<String, Jam>> = MutableLiveData(mapOf())
    val locationLiveData = locationRepository.locationLiveData.map(this::onLocationUpdate)

    private var locationRequest: LocationRequest? = null
    private var isInitialLocation = true
    private var isFirstTrackMeClick: Boolean
        get() = prefsHelper.getValue(FIRST_TRACK_ME_KEY, true)
        set(value) { prefsHelper.saveValue(FIRST_TRACK_ME_KEY, value) }

    init {
        val isFirst = prefsHelper.getValue(IS_FIRST_ENTRANCE_KEY, true)
        isInFirstEntranceSession = MutableLiveData(isFirst)
    }

    val hasLocationPermission: Boolean
        get() {
            val permissionState = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            return permissionState == PackageManager.PERMISSION_GRANTED
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
                (placeLiveData as MutableLiveData).value = place
                place.toLocation()?.also {  updateJamPlacesFor(it) }
                (placeErrorLiveData as MutableLiveData).value = null
            }
            override fun onError(status: Status) {
                (placeErrorLiveData as MutableLiveData).value = if (status.isSuccess || status.isCanceled)
                    null
                else
                    appContext.getString(R.string.problem_with_place)
            }
        })
    }

    fun onLocationPermissionGranted(fragment: Fragment, isGranted: Boolean) {
        if (isGranted) {
            validateClientSettingsForLocation(fragment)
        } else {
            explainNoPermissionConsequence(fragment.findNavController())
        }
    }

    fun onLocateMeClick(v: View, fragment: Fragment, rpl: ActivityResultLauncher<String>) {
        if (v.isActivated) return

        locationRequest =  LocationRequest.create().apply {
            numUpdates = 1
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
        runLocationRequestFlow(fragment, rpl)
    }

    fun handleLocationSettingsResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK)
            locationRequest?.also { locationRepository.runLocationRequest(it) }
    }

    /**
     * @param showExplanationDialog show the user a dialog which explains what this button do dialog. return true if shown
     */
    fun onTrackMeClick(
        v: View,
        fragment: Fragment,
        rpl: ActivityResultLauncher<String>,
        showExplanationDialog: (millis: Long) -> Unit
    ) {
        if (isFirstTrackMeClick) {
            showExplanationDialog(10_000)
            isFirstTrackMeClick = false
            return
        }

        if (v.isActivated){
            locationRepository.stopLocationService()
        } else {
            locationRequest =  LocationRequest.create().apply {
                interval = 10_000
                priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
            }
            runLocationRequestFlow(fragment, rpl)
        }
    }

    fun updateJamPlacesFor(latLng: LatLng) {
        updateJamPlacesFor(latLng.toLocation())
    }

    fun updateJamPlacesFor(location: Location) {
        viewModelScope.launch {
            val a = jamPlacesRepository.getJamPlacesInRadiusAndDays(location)
            (jamPlacesLiveData as MutableLiveData).value = a
        }
    }

    fun onMarkerClick(marker: Marker, navController: NavController) : Boolean {
        val jamId = marker.tag as? String ?: return false
        return jamPlacesLiveData.value?.get(jamId)?.let { jam ->
            val action = FindJamsFragmentDirections.actionFindJamsFragmentToJamTeamDialogFragment(jamId)
            navController.navigate(action)
            true
        } ?: false
    }





    private fun onLocationUpdate(loc: Location?) : Location? {
        if (isInitialLocation && loc != null) {
            val qsLiveData = jamPlacesRepository.jamPlacesQueryStateLiveData
            if (qsLiveData.value == QueryState.Running || qsLiveData.value == QueryState.Idle) {
                qsLiveData.observeForever(object: Observer<QueryState> {
                    override fun onChanged(qs: QueryState?) {
                        if (qs == QueryState.Success || qs is QueryState.Failure) {
                            qsLiveData.removeObserver(this)
                            updateJamPlacesFor(loc)
                        }
                    }
                })
            } else {
                updateJamPlacesFor(loc)
            }
            isInitialLocation = false
        }
        return loc
    }

    private fun runLocationRequestFlow(fragment: Fragment, rpl: ActivityResultLauncher<String>) {
        if (hasLocationPermission) {
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
        val locReq = locationRequest ?: return
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locReq)

        val client: SettingsClient = LocationServices.getSettingsClient(appContext)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            locationRequest?.also { locationRepository.runLocationRequest(it) }
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

    class Factory(
        regOwner: SavedStateRegistryOwner,
        private val appContext: Context
    ) : AbstractSavedStateViewModelFactory(regOwner, null) {
        private val prefsHelperFactory = SharedPrefsHelper.Factory(appContext, GsonContainer.instance)
        override fun <T : ViewModel?> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
            return FindJamsViewModel(
                appContext,
                prefsHelperFactory.create("HomePrefs"),
                handle,
                AppServiceLocator.locationRepository,
                AppServiceLocator.jamPlacesRepository,
                AppServiceLocator.userRepository
            ) as T
        }
    }

    companion object {
        private const val IS_FIRST_ENTRANCE_KEY = "kjdang"
        private const val FIRST_TRACK_ME_KEY = "kjdanISFIRS923Ag"
        const val REQUEST_CHECK_LOCATOIN_SETTINGS = 31
    }
}