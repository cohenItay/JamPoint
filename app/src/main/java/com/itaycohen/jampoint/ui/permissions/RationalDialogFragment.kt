package com.itaycohen.jampoint.ui.permissions

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class RationalDialogFragment : DialogFragment() {

    private val args: RationalDialogFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return MaterialAlertDialogBuilder(requireContext())
            .setMessage(args.rationalModel.message)
            .setPositiveButton(args.rationalModel.positiveBtnText) { dialog, _ ->
                setFragmentResult(REQUEST_RESULT_KEY, Bundle().also { it.putInt(RATIONAL_KEY, RATIONAL_AGREE) })
                dialog.dismiss()
            }
            .setNegativeButton(args.rationalModel.negativeBtnText) { dialog, _ ->
                setFragmentResult(REQUEST_RESULT_KEY, Bundle().also { it.putInt(RATIONAL_KEY, RATIONAL_DISAGREE) })
                dialog.dismiss()
            }
            .setCancelable(false)
            .also {
                if (args.rationalModel.title > 0)
                    it.setTitle(args.rationalModel.title)
            }
            .create()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    companion object {
        const val REQUEST_RESULT_KEY = "ResK23"
        const val RATIONAL_KEY = "RatinasK23"
        const val RATIONAL_AGREE = 1
        const val RATIONAL_DISAGREE = 2
    }
}