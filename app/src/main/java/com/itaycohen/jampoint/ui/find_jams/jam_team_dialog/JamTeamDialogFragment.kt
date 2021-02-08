package com.itaycohen.jampoint.ui.find_jams.jam_team_dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.databinding.FragmentJamTeamBinding
import com.itaycohen.jampoint.utils.UiUtils.convertDpToPx

class JamTeamDialogFragment : BottomSheetDialogFragment() {

    private lateinit var jamTeamViewModel: JamTeamViewModel
    private val args: JamTeamDialogFragmentArgs by navArgs()
    private var _binding: FragmentJamTeamBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vmFactory = JamTeamViewModel.Factory(this, requireContext().applicationContext, args.jamPlaceKey)
        jamTeamViewModel = ViewModelProvider(this, vmFactory).get(JamTeamViewModel::class.java)
    }

    override fun getTheme(): Int {
        return R.style.BottomDialogTheme
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentJamTeamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BottomSheetBehavior.from(binding.root.parent as View).also {
            it.setPeekHeight(binding.root.resources.convertDpToPx(160).toInt())
        }
        binding.rootRecyclerView.apply {
            adapter = JamTeamAdapter(viewLifecycleOwner, jamTeamViewModel, this@JamTeamDialogFragment.findNavController())
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                setDrawable(ContextCompat.getDrawable(context, R.drawable.transparent_rectangle_48dp_height)!!)
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}