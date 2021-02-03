package com.itaycohen.jampoint.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuthProvider
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.databinding.FragmentProfileBinding
import com.itaycohen.jampoint.utils.highQualityPhotoUri

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
    }

    private fun onUserUpdated(user: FirebaseUser?) = with(binding) {
        Glide.with(requireContext())
            .load(user?.highQualityPhotoUri)
            .circleCrop()
            .placeholder(R.drawable.ic_baseline_account_circle_24)
            .error(R.drawable.ic_baseline_account_circle_24)
            .into(binding.profileImage)
        fullNameTextView.text = if (user == null) getString(R.string.hi_guest) else user.displayName
        if (user != null) {
            signBtn.setOnClickListener(viewModel::onSignOut)
            signBtn.setImageResource(R.drawable.ic_baseline_exit_to_app_24)
        } else {
            signBtn.setOnClickListener(viewModel::onSignIn)
            signBtn.setImageResource(R.drawable.ic_baseline_login_24)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}