package com.itaycohen.jampoint.ui.my_jams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.QueryState
import com.itaycohen.jampoint.databinding.FragmentMyJamsBinding

class MyJamsFragment : Fragment() {
    
    private lateinit var viewModel: MyJamsViewModel
    private var _binding : FragmentMyJamsBinding? = null
    private val binding: FragmentMyJamsBinding
        get() = _binding!!
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vmFactory = MyJamsViewModel.Factory(this, requireContext().applicationContext)
        viewModel = ViewModelProvider(this, vmFactory).get(MyJamsViewModel::class.java)
        viewModel.fetchSelfJams()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyJamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        binding.recyclerView.apply {
            adapter = MyJamsAdapter()
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                val drawable = ContextCompat.getDrawable(context, R.drawable.transparent_rectangle_48dp_height)!!
                setDrawable(drawable)
            })
        }
        initInteractions()
    }

    private fun initInteractions() {
        binding.swipeToRefreshLayout.setOnRefreshListener {
            viewModel.fetchSelfJams()
        }
        val params = binding.createJamLayot.layoutParams as CoordinatorLayout.LayoutParams
        (params.behavior as BottomSheetBehavior)?.also { bsb ->
            bsb.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    binding.swipeToRefreshLayout.isEnabled =
                        newState == BottomSheetBehavior.STATE_COLLAPSED ||
                        newState == BottomSheetBehavior.STATE_HIDDEN
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }
            })
        }
    }

    private fun initObservers() {
        viewModel.selfJamsLiveData.observe(viewLifecycleOwner) {
            val adapter = binding.recyclerView.adapter as MyJamsAdapter
            adapter.myJamsList = it?.values?.toList() ?: listOf()
            binding.recyclerView.isVisible = adapter.myJamsList.isNotEmpty()
            adapter.notifyDataSetChanged()
        }
        viewModel.queryStateLiveData.observe(viewLifecycleOwner) {
            binding.swipeToRefreshLayout.isRefreshing = it is QueryState.Running
            binding.errorTextView.apply {
                isVisible = it is QueryState.Failure
                text = (it as? QueryState.Failure)?.errMsg
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}