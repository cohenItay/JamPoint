package com.itaycohen.jampoint.ui.permissions

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.itaycohen.jampoint.R


class NoPermissionDialogFragment : DialogFragment() {

    private val args: NoPermissionDialogFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return MaterialAlertDialogBuilder(requireContext())
            .setMessage(args.noPermissionModel.message)
            .setPositiveButton(args.noPermissionModel.positiveBtnText) { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton(R.string.go_to_app_system_settings) { dialog, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setCancelable(true)
            .also {
                if (args.noPermissionModel.title > 0)
                    it.setTitle(args.noPermissionModel.title)
            }
            .create()
    }
}