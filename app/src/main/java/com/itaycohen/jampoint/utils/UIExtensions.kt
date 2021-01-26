package com.itaycohen.jampoint.utils

import android.content.res.Resources


fun Resources.toDp(px: Float): Float {
    return px / getDisplayMetrics().density
}

fun Resources.toPx(dp: Float): Float {
    return dp * getDisplayMetrics().density
}