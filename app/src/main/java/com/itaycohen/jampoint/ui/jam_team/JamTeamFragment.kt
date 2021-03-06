package com.itaycohen.jampoint.ui.jam_team

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.button.MaterialButton
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.User
import com.itaycohen.jampoint.databinding.FragmentJamTeamBinding
import com.itaycohen.jampoint.ui.jam_team.join_request.JoinTeamDialogFragment
import com.itaycohen.jampoint.utils.DestinationsUtils

class JamTeamFragment : Fragment() {

    private lateinit var jamTeamViewModel: JamTeamViewModel
    private val mutualViewModel: JamTeamMutualViewModel by viewModels({requireParentFragment()})
    private val args: JamTeamFragmentArgs by navArgs()
    private var _binding: FragmentJamTeamBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vmFactory = JamTeamViewModel.Factory(this, requireContext().applicationContext)
        jamTeamViewModel = ViewModelProvider(this, vmFactory).get(JamTeamViewModel::class.java)
        if (args.jamPlaceKey != null) {
            jamTeamViewModel.updateJamPlaceId(args.jamPlaceKey!!)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJamTeamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTopAppBar()
        if (args.jamPlaceKey == null) {
            mutualViewModel.activeJamIdLiveData.observe(viewLifecycleOwner) {
                it ?: return@observe
                jamTeamViewModel.updateJamPlaceId(it)
            }
        }
        binding.rootRecyclerView.apply {
            adapter = JamTeamAdapter(
                viewLifecycleOwner,
                jamTeamViewModel,
                this@JamTeamFragment.findNavController(),
                childFragmentManager
            )
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                setDrawable(ContextCompat.getDrawable(context, R.drawable.transparent_rectangle_48dp_height)!!)
            })
        }
        (binding.isEditModeBtn as MaterialButton).isCheckable = true
        binding.isEditModeBtn.setOnClickListener(jamTeamViewModel::onEditModeClick)
        jamTeamViewModel.isManagerLiveData.observe(viewLifecycleOwner) { isManager ->
            binding.isEditModeBtn.isVisible = isManager
            if (isManager && args.startInEditModeIfPossible)
                jamTeamViewModel.onEditModeClick(binding.isEditModeBtn)
        }
        jamTeamViewModel.isInEditModeLiveData.observe(viewLifecycleOwner) {
            (binding.isEditModeBtn as MaterialButton).isChecked = it
            binding.rootRecyclerView.adapter?.notifyDataSetChanged()
        }
        jamTeamViewModel.userLiveData.observe(viewLifecycleOwner) {
            binding.rootRecyclerView.adapter?.notifyDataSetChanged()
        }
        mutualViewModel.joinTeamResult = { reqKey: String, b: Bundle ->
            b.getBoolean(JoinTeamDialogFragment.REQUEST_JOIN_REQ_SENT_KEY, false).also { reqSent ->
                if (reqSent) {
                    mutualViewModel.activeJamIdLiveData.value?.also { jamTeamViewModel.updateJamPlaceId(it) }
                }
            }
        }
    }

    private fun initTopAppBar() = with (binding.topAppBar) {
        isVisible = !args.isEmbedded
        val appBarConfiguration = AppBarConfiguration(DestinationsUtils.getRootDestinationsSet())
        setupWithNavController(findNavController(), appBarConfiguration)
        title = ""
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}