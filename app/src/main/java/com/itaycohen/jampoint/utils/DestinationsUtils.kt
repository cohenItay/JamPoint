package com.itaycohen.jampoint.utils

import com.itaycohen.jampoint.R

object DestinationsUtils {
    fun getRootDestinationsSet(): Set<Int> {
        return setOf(
            R.id.navigation_home,
            R.id.navigation_dashboard,
            R.id.navigation_notifications
        )
    }
}