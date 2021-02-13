package com.itaycohen.jampoint.ui.jam_team.join_request

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.itaycohen.jampoint.R
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
        check(args.jamPointId.isNotBlank()) { "No valid args" }
        val vmFactory = JoinTeamViewModel.Factory(this, requireContext().applicationContext)
        viewModel = ViewModelProvider(this, vmFactory).get(JoinTeamViewModel::class.java)
        setFragmentResultListener(LoginDialogFragment.REQUEST_SIGN_IN, fragmentResultListener)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val title = getString(if (args.jamMeet != null) R.string.join_meeting else R.string.join_team)
        val message = if (args.jamMeet != null)
            getString(R.string.validate_join_meeting, args.jamMeet?.getUiTime())
        else
            getString(R.string.validate_join_jam_point)
        viewModel.userLiveData.observe(this, userObserver)
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            /*DONT USE BUILDER BUTTONS CLICK LISTENER API - WE DON'T WANT THAT A CLICK WILL AUTO-DISMISS THIS DIALOG*/
            .setPositiveButton(R.string.yes_join, null)
            .setNegativeButton(R.string.back, null)
            .setCancelable(true)
            .create()
    }

    override fun onStart() {
        super.onStart()
        with(requireDialog() as AlertDialog) {
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (args.jamMeet != null) {
                    viewModel.requestToJoin(args.jamPointId, args.jamMeet!!)
                } else {
                    viewModel.requestToJoin(args.jamPointId)
                }
                findNavController().popBackStack()
                setFragmentResult(FRAG_RESULT_KEY, Bundle().apply { putBoolean(REQUEST_JOIN_REQ_SENT_KEY, true) })
            }
            getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                findNavController().popBackStack()
                setFragmentResult(FRAG_RESULT_KEY, Bundle().apply { putBoolean(REQUEST_JOIN_REQ_SENT_KEY, false) })
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val userObserver = Observer { user: User? ->
        if (user == null) {
            val action = JoinTeamDialogFragmentDirections.actionGlobalLoginFragment()
            findNavController().navigate(action)
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

    companion object {
        const val FRAG_RESULT_KEY = "ResJofragrest983"
        const val REQUEST_JOIN_REQ_SENT_KEY = "ResJoinrsnK2tt983"
    }
}