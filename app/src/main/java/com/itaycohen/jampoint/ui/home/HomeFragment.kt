package com.itaycohen.jampoint.ui.home

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.SearchManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.databinding.FragmentHomeBinding
import com.itaycohen.jampoint.utils.DestinationsUtils
import com.itaycohen.jampoint.utils.GsonContainer
import com.itaycohen.jampoint.utils.SharedPrefsHelper
import com.itaycohen.jampoint.utils.toPx

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null
    private var searchObjAnim: ObjectAnimator? = null
    private val binding: FragmentHomeBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vmFactory = HomeViewModel.Factory(this, requireContext().applicationContext)
        homeViewModel = ViewModelProvider(this, vmFactory).get(HomeViewModel::class.java)
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
        with (binding) {
            val appBarConfiguration = AppBarConfiguration(DestinationsUtils.getRootDestinationsSet())
            topAppBar.setupWithNavController(findNavController(), appBarConfiguration)
            (topAppBar.menu.findItem(R.id.searchItem)?.actionView as? SearchView)?.also {
                initSearchView(it)
            }
        }
        with (homeViewModel) {
            isInFirstEntranceSession.observe(viewLifecycleOwner, firstEntranceObserver)
        }
    }

    private fun initSearchView(searchView: SearchView) = with (searchView) {
        val searchManager = requireContext().getSystemService(Context.SEARCH_SERVICE) as SearchManager
        setSearchableInfo(searchManager.getSearchableInfo(requireActivity().componentName))
        isIconifiedByDefault = true
        setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
        })
    }

    private val firstEntranceObserver = Observer<Boolean> { isInSession ->
        val menuItem = binding.topAppBar.menu.findItem(R.id.searchItem) ?: return@Observer
        if (isInSession) {
            menuItem.setOnActionExpandListener(object: MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                    openLocationMethodDialog()
                    return false
                }

                override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                    return true
                }
            })
            animateSearchIcon(binding.topAppBar.findViewById(R.id.searchItem))
            animateMessageBox(binding.messageBox)
        } else {
            menuItem.setOnActionExpandListener(null)
        }
    }

    private fun animateSearchIcon(searchBtn: ActionMenuItemView) {
        searchObjAnim = ObjectAnimator.ofFloat(searchBtn, View.ALPHA, 0f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 500L
            doOnEnd {
                searchBtn.alpha = 1f
            }
            startDelay = 500L
        }
        searchObjAnim!!.start()
    }

    private fun animateMessageBox(messageBox: TextView) = with (messageBox) {
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
        binding.messageBox.isVisible = false
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.search_jams)
            .setMessage(R.string.search_jams_method_explanation)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                homeViewModel.endFirstEntranceSession()
            }
            .setNegativeButton(R.string.no_feed_manually) { dialog, _ ->
                homeViewModel.endFirstEntranceSession()
                binding.topAppBar.findViewById<ActionMenuItemView>(R.id.searchItem)?.callOnClick()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}