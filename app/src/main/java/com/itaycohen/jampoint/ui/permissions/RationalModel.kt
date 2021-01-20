package com.itaycohen.jampoint.ui.permissions

import android.os.Parcelable
import androidx.annotation.StringRes
import com.itaycohen.jampoint.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class RationalModel(
    @StringRes val message: Int,
    @StringRes val title: Int = 0,
    @StringRes val positiveBtnText: Int = R.string.yes,
    @StringRes val negativeBtnText: Int = R.string.no
) : Parcelable