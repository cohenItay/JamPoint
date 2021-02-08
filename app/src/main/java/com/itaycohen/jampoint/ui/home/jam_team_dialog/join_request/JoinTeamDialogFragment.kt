package com.itaycohen.jampoint.ui.home.jam_team_dialog.join_request

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseUser
import com.itaycohen.jampoint.data.models.User
import com.itaycohen.jampoint.databinding.FragmentJoinTeamDialogBinding
import com.itaycohen.jampoint.ui.sign_up.LoginDialogFragment

class JoinTeamDialogFragment : DialogFragment() {

    private lateinit var viewModel: JoinTeamViewModel
    private val args: JoinTeamDialogFragmentArgs by navArgs()
    private var _binding : FragmentJoinTeamDialogBinding? = null
    private val binding: FragmentJoinTeamDialogBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check((args.jamMeet != null || !args.jamPointId.isNullOrBlank()) &&
                !(args.jamMeet != null && args.jamPointId.isNullOrBlank()) ) {
            "No valid args at all or both of the args supplied, if so only one of them should be provided"
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val userObserver = Observer { user: User? ->
        if (user == null) {
            val action = JoinTeamDialogFragmentDirections.actionJoinTeamDialogFragmentToLoginDialogFragment()
            findNavController().navigate(action)
        }
        requireView().isVisible = user != null
    }

    private val fragmentResultListener = { requestKey: String, bundle: Bundle ->
        if (requestKey == LoginDialogFragment.KEY_SIGN_IN_SUCCESS) {
            val isSignedIn = bundle.getBoolean(LoginDialogFragment.KEY_SIGN_IN_SUCCESS, false)
            if (isSignedIn) {
                if (args.jamMeet != null) {
                    viewModel.requestToJoin(args.jamMeet!!)
                } else {
                    viewModel.requestToJoin(args.jamPointId!!)
                }
            } else {
                findNavController().popBackStack()
            }
        }
    }

    companion object {
        const val REQUEST_SIGN_IN = "ResK2tt983"
        const val KEY_SIGN_IN = "ResK2tt983"
    }
}