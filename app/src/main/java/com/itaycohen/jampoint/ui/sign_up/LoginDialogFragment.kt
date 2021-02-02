package com.itaycohen.jampoint.ui.sign_up

import android.app.Dialog
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.itaycohen.jampoint.R
import com.itaycohen.jampoint.data.UserResult

class LoginDialogFragment : DialogFragment() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var loginResultLauncher: ActivityResultLauncher<Unit?>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
       return  MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.login_needed)
            .setMessage(R.string.why_to_login_explanation)
            .setPositiveButton(R.string.ok_sign_in) { dialog, _ ->
                loginResultLauncher.launch(null)
            }
            .setNegativeButton(R.string.back) { dialog, _ ->
                setFragmentResult(REQUEST_SIGN_IN, Bundle().apply { putBoolean(KEY_SIGN_IN_SUCCESS, false) } )
                findNavController().popBackStack()
            }
            .setCancelable(false)
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vmFactory = LoginViewModel.Factory(this, requireContext().applicationContext)
        viewModel = ViewModelProvider(this, vmFactory).get(LoginViewModel::class.java)
        loginResultLauncher = registerForActivityResult(viewModel.createFirebaseLoginContract(), onUserLoginActivityResult)
    }

    private val onUserLoginActivityResult = { userResult: UserResult ->
        findNavController().popBackStack()
        setFragmentResult(REQUEST_SIGN_IN, Bundle().apply { putBoolean(KEY_SIGN_IN_SUCCESS, userResult.user != null) } )
        Unit
    }

    companion object {
        const val REQUEST_SIGN_IN = "ResK2tt983"
        const val KEY_SIGN_IN_SUCCESS = "ResK2tt983"
    }
}