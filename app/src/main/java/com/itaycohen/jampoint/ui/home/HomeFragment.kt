package com.itaycohen.jampoint.ui.home

import android.R.attr.radius
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.snackbar.Snackbar
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.databinding.FragmentHomeBinding
import com.itaycohen.jampoint.utils.DestinationsUtils
import com.itaycohen.jampoint.utils.toPx


class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null
    private var searchObjAnim: ObjectAnimator? = null
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>
    private val binding: FragmentHomeBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vmFactory = HomeViewModel.Factory(this, requireContext().applicationContext)
        homeViewModel = ViewModelProvider(this, vmFactory).get(HomeViewModel::class.java)
        val resultCallback = homeViewModel.createLocationActivityResultCallback { findNavController() }
        locationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission(), resultCallback)
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
        with(homeViewModel) {
            isInFirstEntranceSession.observe(viewLifecycleOwner, firstEntranceObserver)
            placeErrorLiveData.observe(viewLifecycleOwner) { errMsg ->
                if (errMsg != null)
                    Snackbar.make(requireView(), errMsg, Snackbar.LENGTH_SHORT).show()
            }
        }
        binding.locateFab.setOnClickListener {
            homeViewModel.trackUserOrlaunchLocationPermissionLogic(this, locationPermissionLauncher)
        }
    }

    override fun onStart() {
        super.onStart()
        addPlacesFragment()
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
        if (homeViewModel.isInFirstEntranceSession.value!!) {
            binding.toolbarMaskView.setOnClickListener {
                openLocationMethodDialog()
                binding.toolbarMaskView.setOnClickListener(null)
            }
        }
    }

    private fun addPlacesFragment() {
        val placesFragment = (childFragmentManager.findFragmentById(R.id.placesFragmentContainer) as? AutocompleteSupportFragment)
        val frag = placesFragment ?: AutocompleteSupportFragment.newInstance().also {
            homeViewModel.initPlacesFragmentConfiguration(it)
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
                toolbar.alpha = 1f
                binding.messageBox.animate()
                    .alpha(0f)
                    .setDuration(1000L)
                    .setListener(doOnEnd {
                        binding.messageBox.isVisible = false
                    })
                    .start()
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
                homeViewModel.endFirstEntranceSession()
                homeViewModel.trackUserOrlaunchLocationPermissionLogic(this, locationPermissionLauncher)
            }
            .setNegativeButton(R.string.no_feed_manually) { dialog, _ ->
                homeViewModel.endFirstEntranceSession()
                val placesFrag = childFragmentManager.findFragmentById(R.id.placesFragmentContainer)
                (placesFrag as? AutocompleteSupportFragment)?.also {
                    it.requireView().findViewById<View>(R.id.places_autocomplete_search_input).callOnClick()
                }
            }
            .setCancelable(false)
            .show()
    }
}