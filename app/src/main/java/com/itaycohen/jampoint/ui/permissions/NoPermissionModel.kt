package com.itaycohen.jampoint.ui.permissions

import android.os.Parcelable
import androidx.annotation.StringRes
import com.itaycohen.jampoint.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class NoPermissionModel(
    @StringRes val message: Int,
    @StringRes val title: Int = R.string.location_permission_denied,
    @StringRes val positiveBtnText: Int = R.string.understood,
) : Parcelable