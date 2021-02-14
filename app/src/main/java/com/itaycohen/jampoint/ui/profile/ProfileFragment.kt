package com.itaycohen.jampoint.ui.profile

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.models.QueryState
import com.itaycohen.jampoint.data.models.User
import com.itaycohen.jampoint.databinding.FragmentProfileBinding
import com.itaycohen.jampoint.utils.UiUtils

class ProfileFragment : Fragment() {
    
    private lateinit var viewModel: ProfileViewModel
    private var _binding : FragmentProfileBinding? = null
    private val binding: FragmentProfileBinding
        get() = _binding!!
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vmFactory = ProfileViewModel.Factory(this, requireContext().applicationContext)
        viewModel = ViewModelProvider(this, vmFactory).get(ProfileViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.userLiveData.observe(viewLifecycleOwner, ::onUserUpdated)
        viewModel.instrumentQueryState.observe(viewLifecycleOwner, insrumentQuerystateObserver)
        binding.instrumentTextView.setOnClickListener { setEditInstrumentMode(true) }
        binding.root.setOnClickListener { binding.instrumentTextInputLayout.editText!!.clearFocus() }
        setEditInstrumentMode(false)
        binding.instrumentTextInputLayout.editText!!.setOnEditorActionListener(::onInstrumentActionGo)
        binding.instrumentTextInputLayout.editText!!.doOnTextChanged { text, start, before, count ->
            binding.instrumentTextInputLayout.error = null
        }
        binding.instrumentTextInputLayout.editText!!.setOnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                setEditInstrumentMode(false)
            }
        }
    }

    private fun onInstrumentActionGo(v: View, actionId: Int?, event: KeyEvent?) : Boolean {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            val instrument = (v as EditText).text.toString()
            if (instrument.isNotBlank()) {
                viewModel.updateUserInstrument(instrument)
                return true
            } else {
                binding.instrumentTextInputLayout.error = getString(R.string.input_is_empty)
            }
        }
        return false
    }

    private fun setEditInstrumentMode(isEditing: Boolean) {
        binding.instrumentTextView.isVisible = !isEditing
        binding.instrumentTextInputLayout.isVisible = isEditing
        if (isEditing)
            binding.instrumentTextInputLayout.editText!!.requestFocus()
    }

    private val insrumentQuerystateObserver = Observer { qs: QueryState ->
        if (qs is QueryState.Running)
            binding.insreumentProgressBar.show()
        else if (qs is QueryState.Failure && qs.errMsg != null) {
            binding.instrumentTextInputLayout.editText!!.setText(viewModel.userLiveData.value?.mainInstrument)
            Snackbar.make(requireView(), qs.errMsg, Snackbar.LENGTH_LONG).show()
        } else {
            binding.insreumentProgressBar.hide()
            binding.instrumentTextInputLayout.editText!!.clearFocus()
            binding.instrumentTextView.text = viewModel.userLiveData.value?.mainInstrument
            UiUtils.hideKeyboard(requireView())
        }
    }

    private fun onUserUpdated(user: User?) = with(binding) {
        Glide.with(requireContext())
            .load(user?.profileImageUrl)
            .circleCrop()
            .placeholder(R.drawable.ic_baseline_account_circle_24)
            .error(R.drawable.ic_baseline_account_circle_24)
            .into(binding.profileImage)
        fullNameTextView.text = user?.fullName ?: getString(R.string.hi_guest)
        instrumentTextInputLayout.editText!!.setText(user?.mainInstrument)
        instrumentTextView.text = user?.mainInstrument
        if (user != null) {
            signBtn.setOnClickListener(viewModel::onSignOut)
            signBtn.setImageResource(R.drawable.ic_baseline_exit_to_app_24)
        } else {
            signBtn.setOnClickListener(viewModel::onSignIn)
            signBtn.setImageResource(R.drawable.ic_baseline_login_24)
        }
        binding.instrumentTextView.isVisible = user != null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}