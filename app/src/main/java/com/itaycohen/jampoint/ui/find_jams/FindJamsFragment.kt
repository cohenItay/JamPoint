package com.itaycohen.jampoint.ui.find_jams

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.Jam
import com.itaycohen.jampoint.data.models.ServiceState
import com.itaycohen.jampoint.databinding.FragmentFindJamsBinding
import com.itaycohen.jampoint.ui.jam_team.JamTeamFragment
import com.itaycohen.jampoint.ui.jam_team.JamTeamMutualViewModel
import com.itaycohen.jampoint.ui.jam_team.join_request.JoinTeamDialogFragment
import com.itaycohen.jampoint.utils.DestinationsUtils
import com.itaycohen.jampoint.utils.toLatLng
import com.itaycohen.jampoint.utils.toPx


class FindJamsFragment : Fragment() {

    private lateinit var findJamsViewModel: FindJamsViewModel
    private val jamTeamMutualViewModel : JamTeamMutualViewModel by viewModels()
    private var googleMap: GoogleMap? = null
    private var _binding: FragmentFindJamsBinding? = null
    private var searchObjAnim: ObjectAnimator? = null
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    private val jamPlacesMarkers = mutableListOf<Marker>()
    private var isLocateSelfCameraMove: Boolean = false
    private var markSelfLocationRunnable: (() -> Unit)? = null
    private var markJamPointsRunnable: (() -> Unit)? = null
    private val binding: FragmentFindJamsBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vmFactory = FindJamsViewModel.Factory(this, requireContext().applicationContext)
        findJamsViewModel = ViewModelProvider(this, vmFactory).get(FindJamsViewModel::class.java)
        setFragmentResultListener(JoinTeamDialogFragment.FRAG_RESULT_KEY) { key: String, b: Bundle ->
            jamTeamMutualViewModel.joinTeamResult?.invoke(key, b)
        }
        locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            findJamsViewModel.onLocationPermissionGranted(this, isGranted)
        }
        savedInstanceState?.also { isLocateSelfCameraMove = it.getBoolean(LOCATE_SELF_KEY) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFindJamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTopAppBar()
        val jamTeamFrag = JamTeamFragment().apply {
            arguments = Bundle().apply {
                putBoolean("isEmbedded", true)
            }
        }
        childFragmentManager.beginTransaction()
            .replace(binding.bottomFragmentContainer.id, jamTeamFrag)
            .commit()
        BottomSheetBehavior.from(binding.bottomContainer).state = BottomSheetBehavior.STATE_HIDDEN
        initObservers()
        initInteractionListeners()
        val mapFrag = childFragmentManager.findFragmentById(R.id.mapFragmentContainer) as SupportMapFragment
        mapFrag.getMapAsync { initGoogleMaps(it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // I must use this deprecate callback, because [ResolvableApiException.startResolutionForResult]
        // Doesn't support [ActivityResultContract] yet.
        if (requestCode == FindJamsViewModel.REQUEST_CHECK_LOCATOIN_SETTINGS)
            findJamsViewModel.handleLocationSettingsResult(resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        addPlacesFragment()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(LOCATE_SELF_KEY, isLocateSelfCameraMove)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    private fun initTopAppBar() = with(binding.topAppBar) {
        val appBarConfiguration = AppBarConfiguration(DestinationsUtils.getRootDestinationsSet())
        setupWithNavController(findNavController(), appBarConfiguration)
        (background as MaterialShapeDrawable).apply {
            shapeAppearanceModel = shapeAppearanceModel
                .toBuilder()
                .setAllCorners(CornerFamily.ROUNDED, android.R.attr.radius.toFloat())
                .build()
            fillColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
            val typedValue = TypedValue()
            context.theme.resolveAttribute(R.attr.colorPrimary, typedValue, true)
            strokeColor = ColorStateList.valueOf(typedValue.data)
            strokeWidth = 6f
        }
        if (findJamsViewModel.isInFirstEntranceSession.value!!) {
            binding.toolbarMaskView.setOnClickListener {
                openLocationMethodDialog()
                binding.toolbarMaskView.setOnClickListener(null)
                binding.toolbarMaskView.isVisible = false
            }
        }
    }

    private fun initObservers() = with(findJamsViewModel) {
        isInFirstEntranceSession.observe(viewLifecycleOwner, firstEntranceObserver)
        placeErrorLiveData.observe(viewLifecycleOwner) { errMsg ->
            if (errMsg != null)
                Snackbar.make(requireView(), errMsg, Snackbar.LENGTH_SHORT).show()
        }
        serviceStateLiveData.observe(viewLifecycleOwner) { servicestate ->
            view ?: return@observe
            when (servicestate) {
                is ServiceState.Available -> {
                    binding.trackMeFab.isActivated = true
                    binding.locateFab.isEnabled = false
                }
                is ServiceState.Idle -> {
                    binding.trackMeFab.isActivated = false
                    binding.locateFab.isEnabled = true
                }
                is ServiceState.Unavailable -> {
                    binding.trackMeFab.isActivated = false
                    binding.locateFab.isEnabled = true
                    Snackbar.make(requireView(), R.string.problem_with_location_updates, Snackbar.LENGTH_LONG).apply {
                        show()
                    }
                }
            }
        }
        locationLiveData.observe(viewLifecycleOwner) { location ->
            view ?: return@observe
            location ?: return@observe
            isLocateSelfCameraMove = true
            val latLng = LatLng(location.latitude, location.longitude)
            markSelfLocationRunnable = {
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                updateSelfMarker(latLng)
            }
            googleMap?.also {
                markSelfLocationRunnable!!()
                markSelfLocationRunnable = null
            }
        }
        findJamsViewModel.placeLiveData.observe(viewLifecycleOwner) {
            val placeLatLng = it?.latLng ?: return@observe
            googleMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(placeLatLng, 15f)
            )
        }
        placeErrorLiveData.observe(viewLifecycleOwner) {
            val errMsg = it ?: return@observe
            Snackbar.make(requireView(), errMsg, Snackbar.LENGTH_SHORT).show()
        }
        jamPlacesLiveData.observe(viewLifecycleOwner) {
            markJamPointsRunnable = { updateJamMarkers(it) }
            googleMap?.let {
                markJamPointsRunnable!!()
                markJamPointsRunnable = null
            }
        }
    }

    private fun updateJamMarkers(futureJamsMap: Map<String, Jam>?) {
        val map = googleMap ?: return
        map.clear()
        findJamsViewModel.locationLiveData.value?.also {
            updateSelfMarker(it.toLatLng())
        }
        jamPlacesMarkers.clear()
        futureJamsMap ?: return
        val newMarkers: Collection<Marker> = futureJamsMap.flatMap { entry ->
            val jamPlace = entry.value
            val jamMeetingsCollection = jamPlace.jamMeetings?.values
            val latLngsSet = mutableSetOf<LatLng>()
            jamMeetingsCollection?.forEach {
                it.toLatLng()?.also {  latLngsSet.add(it) }
            }
            val uniqueLocationMeeting = jamMeetingsCollection?.filter {
                val latLng = it.toLatLng()
                val drawThisMeet = latLng != null && latLngsSet.contains(latLng)
                latLngsSet.remove(latLng)
                drawThisMeet
            }
            uniqueLocationMeeting?.map {
                val latLng = it.toLatLng()
                latLng?.let {
                    val marker = map.addMarker(MarkerOptions().apply {
                        position(it)
                        title(jamPlace.jamPlaceNickname)
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.ic_baseline_location_on_24
                        )?.apply {
                            setTint(
                                ContextCompat.getColor(
                                    requireContext(),
                                    if (jamPlace.isLive == true) R.color.purple300 else R.color.purple_gray300
                                )
                            )
                        }?.toBitmap()?.also { bitmap ->
                            icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                        }
                    })
                    marker.tag = entry.key
                    marker
                }
                null
            } ?: listOf()
        }.filterNotNull()
        jamPlacesMarkers.addAll(newMarkers)
    }

    private fun initInteractionListeners() = with(binding){
        binding.trackMeFab.setOnClickListener {
            findJamsViewModel.onTrackMeClick(
                it,
                this@FindJamsFragment,
                locationPermissionLauncher,
                this@FindJamsFragment::showTrackMeExplanation
            )
        }
        binding.locateFab.setOnClickListener {
            findJamsViewModel.onLocateMeClick(it, this@FindJamsFragment, locationPermissionLauncher)
        }
        val params = binding.bottomContainer.layoutParams as CoordinatorLayout.LayoutParams
        (params.behavior as? BottomSheetBehavior)?.also { bsb ->
            bsb.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {

                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }
            })
        }
    }

    private fun initGoogleMaps(googleMap: GoogleMap) = with(googleMap) {
        this@FindJamsFragment.googleMap = this
        setMinZoomPreference(8f)
        setMaxZoomPreference(17f)
        initMapInteractionListeners(this)
        markSelfLocationRunnable?.invoke()
        markSelfLocationRunnable = null
        markJamPointsRunnable?.invoke()
        markJamPointsRunnable = null
    }

    private fun initMapInteractionListeners(googleMap: GoogleMap) = with(googleMap) {
        setOnCameraMoveStartedListener {
            binding.locateFab.isActivated = false
        }
        setOnCameraIdleListener {
            if (isLocateSelfCameraMove) {
                if (binding.locateFab.isEnabled)
                    binding.locateFab.isActivated = true
                isLocateSelfCameraMove = false
            }
            Log.d("yyy", "setOnCameraIdleListener: ")
            findJamsViewModel.updateJamPlacesFor(googleMap.projection.visibleRegion.latLngBounds.center)
        }
        setOnMarkerClickListener {
            BottomSheetBehavior.from(binding.bottomContainer).state = BottomSheetBehavior.STATE_COLLAPSED
            return@setOnMarkerClickListener jamTeamMutualViewModel.onMarkerClick(it)
        }
    }

    private fun updateSelfMarker(latLng: LatLng) {
        val map = googleMap ?: return
        map.addMarker(MarkerOptions().apply {
            position(latLng)
            ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_baseline_person_pin_24
            )?.toBitmap()?.also { bitmap ->
                icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            }
        })
    }

    private fun addPlacesFragment() {
        val placesFragment = (childFragmentManager.findFragmentById(R.id.placesFragmentContainer) as? AutocompleteSupportFragment)
        val frag = placesFragment ?: AutocompleteSupportFragment.newInstance().also {
            findJamsViewModel.initPlacesFragmentConfiguration(it)
            it.setHint(getString(R.string.search_jams))
        }
        childFragmentManager
            .beginTransaction()
            .replace(R.id.placesFragmentContainer, frag)
            .commit()
    }

    private val firstEntranceObserver = Observer<Boolean> { isInSession ->
        if (isInSession) {
            animateSearchIcon(binding.topAppBar)
            animateMessageBox(binding.messageBox)
        }
        binding.mapFragmentContainer.isVisible = !isInSession
    }

    private fun animateSearchIcon(toolbar: MaterialToolbar) {
        searchObjAnim = ObjectAnimator.ofFloat(toolbar, View.ALPHA, 1f, 0f).apply {
            repeatCount = 3
            repeatMode = ValueAnimator.REVERSE
            duration = 1000L
            doOnEnd {
                _binding?.topAppBar?.alpha = 1f
                _binding?.messageBox?.animate()
                    ?.alpha(0f)
                    ?.setDuration(1000L)
                    ?.setListener(doOnEnd {
                        _binding?.messageBox?.isVisible = false
                    })
                    ?.start()
            }
            startDelay = 500L
        }
        searchObjAnim!!.start()
    }

    private fun animateMessageBox(messageBox: TextView) = with(messageBox) {
        isVisible = true
        alpha = 0f
        animate()
            .translationZ(resources.toPx(8f))
            .alpha(1f)
            .setDuration(600L)
            .setStartDelay(500L)
            .start()
    }

    private fun openLocationMethodDialog() {
        searchObjAnim?.cancel()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.search_jams)
            .setMessage(R.string.search_jams_method_explanation)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                findJamsViewModel.endFirstEntranceSession()
                binding.locateFab.callOnClick()
            }
            .setNegativeButton(R.string.no_feed_manually) { dialog, _ ->
                findJamsViewModel.endFirstEntranceSession()
                openPlacesFragmentManually()
            }
            .setCancelable(false)
            .show()
    }

    private fun openPlacesFragmentManually() {
        val placesFrag = childFragmentManager.findFragmentById(R.id.placesFragmentContainer)
        (placesFrag as? AutocompleteSupportFragment)?.also {
            it.requireView().findViewById<View>(R.id.places_autocomplete_search_input).callOnClick()
        }
    }

    private fun showTrackMeExplanation(intervalMillis: Long) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.track_me_mode)
            .setMessage(getString(R.string.track_me_massage, (intervalMillis / 1000).toString()))
            .setPositiveButton(R.string.understood) { _, _ ->
                binding.trackMeFab.callOnClick()
            }
            .setCancelable(true)
            .show()
    }

    companion object {
        private const val LOCATE_SELF_KEY = "focus"
        private val TAG = FindJamsFragment::class.simpleName
    }
}