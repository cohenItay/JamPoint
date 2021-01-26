package com.itaycohen.jampoint.ui.home

import android.R.attr.radius
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.ServiceState
import com.itaycohen.jampoint.databinding.FragmentHomeBinding
import com.itaycohen.jampoint.utils.DestinationsUtils
import com.itaycohen.jampoint.utils.toPx


class FindJamsFragment : Fragment() {

    private lateinit var findJamsViewModel: FindJamsViewModel
    private var googleMap: GoogleMap? = null
    private var _binding: FragmentHomeBinding? = null
    private var searchObjAnim: ObjectAnimator? = null
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    private var selfMarker: Marker? = null
    private var latLng: LatLng? = null
    private var isLocateSelfCameraMove: Boolean = false
    private val binding: FragmentHomeBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vmFactory = FindJamsViewModel.Factory(this, requireContext().applicationContext)
        findJamsViewModel = ViewModelProvider(this, vmFactory).get(FindJamsViewModel::class.java)
        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            findJamsViewModel.createLocationActivityResultCallback { this }
        )
        savedInstanceState?.also { isLocateSelfCameraMove = it.getBoolean(LOCATE_SELF_KEY) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTopAppBar()
        binding.bottomNavigationView.setupWithNavController(findNavController())
        initObservers()
        initInteracionListeners()
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



    private fun initTopAppBar() = with (binding.topAppBar) {
        val appBarConfiguration = AppBarConfiguration(DestinationsUtils.getRootDestinationsSet())
        setupWithNavController(findNavController(), appBarConfiguration)
        val materialShapeDrawable = background as MaterialShapeDrawable
        materialShapeDrawable.shapeAppearanceModel = materialShapeDrawable.shapeAppearanceModel
            .toBuilder()
            .setAllCorners(CornerFamily.ROUNDED, radius.toFloat())
            .build()
        if (findJamsViewModel.isInFirstEntranceSession.value!!) {
            binding.toolbarMaskView.setOnClickListener {
                openLocationMethodDialog()
                binding.toolbarMaskView.setOnClickListener(null)
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
                is ServiceState.Unavailable ->
                    Snackbar.make(requireView(), R.string.problem_with_location_updates, Snackbar.LENGTH_LONG).apply {
                        setAnchorView(binding.bottomNavigationView)
                        show()
                    }
            }
        }
        locationLiveData.observe(viewLifecycleOwner) { location ->
            view ?: return@observe
            location ?: return@observe
            isLocateSelfCameraMove = true
            latLng = LatLng(location.latitude, location.longitude)
            updateSelfMarker()
            googleMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(latLng, 15f)
            )
        }
        findJamsViewModel.serviceStateLiveData.observe(viewLifecycleOwner) {
            when (it) {
                is ServiceState.Available -> {
                    binding.trackMeFab.isActivated = true
                    binding.locateFab.isEnabled = false
                }
                is ServiceState.Unavailable,
                is ServiceState.Idle -> {
                    binding.trackMeFab.isActivated = false
                    binding.locateFab.isEnabled = true
                }
            }
        }
    }

    private fun initInteracionListeners() = with (binding){
        binding.trackMeFab.setOnClickListener {
            if (it.isActivated){
                findJamsViewModel.stopTrackSelf()
            } else {
                findJamsViewModel.trackSelf(this@FindJamsFragment, locationPermissionLauncher)
            }
        }
        binding.locateFab.setOnClickListener {
            if (it.isActivated)
                return@setOnClickListener

            findJamsViewModel.locateSelf(this@FindJamsFragment, locationPermissionLauncher)
        }
    }

    private fun initGoogleMaps(googleMap: GoogleMap) = with (googleMap) {
        this@FindJamsFragment.googleMap = this
        setMinZoomPreference(10f)
        setMaxZoomPreference(17f)
        updateSelfMarker()
        initMapInteractionListeners(this)
    }

    private fun initMapInteractionListeners(googleMap: GoogleMap) = with (googleMap) {
        setOnCameraMoveStartedListener {
            binding.locateFab.isActivated = false
        }
        setOnCameraIdleListener {
            if (isLocateSelfCameraMove) {
                if (binding.locateFab.isEnabled)
                    binding.locateFab.isActivated = true
                isLocateSelfCameraMove = false
            }
        }
    }

    private fun updateSelfMarker() {
        val latLng = this.latLng ?: return
        if (selfMarker == null) {
            val marker = googleMap?.addMarker(MarkerOptions().apply {
                position(latLng)
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_baseline_person_pin_24
                )?.toBitmap()?.also { bitmap ->
                    icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                }
            })
            marker?.also {
                selfMarker = it
            }
        }
        selfMarker!!.position = latLng
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
                findJamsViewModel.locateSelf(this, locationPermissionLauncher)
            }
            .setNegativeButton(R.string.no_feed_manually) { dialog, _ ->
                findJamsViewModel.endFirstEntranceSession()
                val placesFrag = childFragmentManager.findFragmentById(R.id.placesFragmentContainer)
                (placesFrag as? AutocompleteSupportFragment)?.also {
                    it.requireView().findViewById<View>(R.id.places_autocomplete_search_input).callOnClick()
                }
            }
            .setCancelable(false)
            .show()
    }

    companion object {
        private const val LOCATE_SELF_KEY = "focus"
    }
}