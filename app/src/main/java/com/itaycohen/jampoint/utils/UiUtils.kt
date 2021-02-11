package com.itaycohen.jampoint.utils

import android.content.res.Resources
import android.view.ViewGroup
import kotlin.math.absoluteValue

object UiUtils {

    fun syncViewGroupChildToAmount(
        viewGroup: ViewGroup,
        dataItemsAmount: Int,
        addView: (toIndex: Int) -> Unit
    ) {
        val delta = dataItemsAmount - viewGroup.childCount
        if (delta > 0) {
            // Add views:
            val initialCount = viewGroup.childCount
            for (i in 0 until delta)
                addView(initialCount+i)
        } else if (delta < 0) {
            // Remove views:
            for (i in 0 until delta.absoluteValue)
                viewGroup.removeViewAt(viewGroup.childCount-1)
        }
    }

    fun Resources.convertDpToPx(dp: Int) = (dp * (displayMetrics.density))

    fun Resources.convertPxToDp(px: Int) = (px / displayMetrics.density)
}