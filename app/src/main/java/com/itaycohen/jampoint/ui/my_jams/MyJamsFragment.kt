package com.itaycohen.jampoint.ui.my_jams

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.Jam
import com.itaycohen.jampoint.data.models.QueryState
import com.itaycohen.jampoint.databinding.FragmentMyJamsBinding
import com.itaycohen.jampoint.ui.views.MyTextInputEditText
import com.itaycohen.jampoint.utils.TransitionListener
import com.itaycohen.jampoint.utils.UiUtils

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
                toggleFabWithTextInput(true)
            } else {
                val action = MyJamsFragmentDirections.actionGlobalLoginFragment()
                findNavController().navigate(action)
            }
        }
        (binding.nickNameTextInputLayout.editText as MyTextInputEditText).apply {
            setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    toggleFabWithTextInput(false)
                }
            }
            listener = MyTextInputEditText.Listener {
                toggleFabWithTextInput(false)
            }
            setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    UiUtils.hideKeyboard(this)
                    MaterialAlertDialogBuilder(context)
                        .setCancelable(false)
                        .setMessage(getString(R.string.validate_jam_point_nick_name, this.text.toString()))
                        .setPositiveButton(R.string.yes_proceed) { _, _ ->
                            viewModel.createNewJamPoint(this.text.toString()) { newJamPoint ->
                                if (newJamPoint != null)
                                    showEnterTeamFragementDialog(newJamPoint)
                            }
                            toggleFabWithTextInput(false)
                        }
                        .setNegativeButton(R.string.no) { _, _ ->
                            toggleFabWithTextInput(false)
                        }
                        .show()
                    return@setOnEditorActionListener true
                }
                false
            }
        }
    }

    private fun toggleFabWithTextInput(showTextInput: Boolean) {
        val transition = MaterialContainerTransform().apply {
            startView = if (showTextInput) binding.createJamPointFab else binding.nickNameTextInputLayout
            endView = if (showTextInput ) binding.nickNameTextInputLayout else binding.createJamPointFab
            fadeMode = MaterialContainerTransform.FADE_MODE_CROSS
            duration = 400L
            drawingViewId = binding.transformContainer.id
            if (showTextInput) {
                addListener(object: TransitionListener {
                    override fun onTransitionEnd(transition: Transition) {
                        binding.nickNameTextInputLayout.editText?.also {
                            it.requestFocus()
                            UiUtils.showKeyboard(it)
                        }
                    }
                })
            }
        }
        TransitionManager.beginDelayedTransition(binding.transformContainer, transition)
        binding.createJamPointFab.isVisible = !showTextInput
        binding.nickNameTextInputLayout.isVisible = showTextInput
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
        viewModel.userLiveData.observe(viewLifecycleOwner) {
            viewModel.fetchSelfJams()
        }
    }

    private fun showEnterTeamFragementDialog(newJamPoint: Jam) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.you_have_created_new_jam_point)
            .setMessage(R.string.new_jam_point_explanation)
            .setCancelable(false)
            .setPositiveButton(R.string.yes) { _, _ ->
                newJamPoint.jamPointId?.also { viewModel.onJamPlaceClick(requireView(), it) }
            }
            .setNegativeButton(R.string.no_add_later) { _, _ -> }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}