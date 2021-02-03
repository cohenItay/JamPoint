package com.itaycohen.jampoint.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.databinding.FragmentHomeContainerBinding

class MainFragment : Fragment() {

    private val childNavController: NavController by lazy {
        (childFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment).navController
    }
    private val parentNavController: NavController by lazy { findNavController() }
    private lateinit var mainViewModel: MainViewModel
    private var binding: FragmentHomeContainerBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vmFactory = MainViewModel.Factory(this, requireContext().applicationContext)
        mainViewModel = ViewModelProvider(this, vmFactory).get(MainViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentHomeContainerBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.bottomNavigationView.setupWithNavController(childNavController)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}