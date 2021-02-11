package com.itaycohen.jampoint.ui.my_jams

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
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
            adapter = MyJamsAdapter(viewModel)
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
        binding.createJamPointFab.setOnClickListener {
            if (viewModel.userLiveData.value != null) {
                binding.createJamPointFab.isExpanded = true
            } else {
                val action = MyJamsFragmentDirections.actionGlobalLoginFragment()
                findNavController().navigate(action)
            }
        }
        binding.nickNameTextInputLayout.editText!!.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                v as EditText
                val nickName = v.text.toString()
                if (nickName.isNotBlank()) {
                    viewModel.createNewJamPoint(nickName)
                }
                return@setOnEditorActionListener true
            }
            binding.createJamPointFab.isExpanded = false
            false
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