package com.itaycohen.jampoint.ui.home.jam_team_dialog.join_request

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseUser
import com.itaycohen.jampoint.databinding.FragmentJoinTeamDialogBinding
import com.itaycohen.jampoint.ui.sign_up.LoginDialogFragment

class JoinTeamDialogFragment : DialogFragment() {

    private lateinit var viewModel: JoinTeamViewModel
    private var _binding : FragmentJoinTeamDialogBinding? = null
    private val binding: FragmentJoinTeamDialogBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vmFactory = JoinTeamViewModel.Factory(this, requireContext().applicationContext)
        viewModel = ViewModelProvider(this, vmFactory).get(JoinTeamViewModel::class.java)
        setFragmentResultListener(LoginDialogFragment.REQUEST_SIGN_IN, fragmentResultListener)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJoinTeamDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.userLiveData.observe(viewLifecycleOwner, userObserver)
    }

    private val userObserver = Observer { user: FirebaseUser? ->
        if (user == null) {
            val action = JoinTeamDialogFragmentDirections.actionJoinTeamDialogFragmentToLoginDialogFragment()
            findNavController().navigate(action)
        } else {

        }
    }

    private val fragmentResultListener = { requestKey: String, bundle: Bundle ->
        if (requestKey == LoginDialogFragment.KEY_SIGN_IN_SUCCESS) {
            val isSignedIn = bundle.getBoolean(LoginDialogFragment.KEY_SIGN_IN_SUCCESS, false)
            if (!isSignedIn) {
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_SIGN_IN = "ResK2tt983"
        const val KEY_SIGN_IN = "ResK2tt983"
    }
}