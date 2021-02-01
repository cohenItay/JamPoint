package com.itaycohen.jampoint.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.util.Log
import java.util.*


object LocationUtils {
    
    private val TAG = LocationUtils::class.java.simpleName

    fun getCompleteAddressString(ctx: Context, location: Location) =
        getCompleteAddressString(ctx, location.latitude, location.longitude)

    fun getCompleteAddressString(ctx: Context, lat: Double, long: Double): String? {
        var strAdd: String? = null
        val geocoder = Geocoder(ctx, Locale.getDefault())
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(lat, long, 1)
            if (addresses != null) {
                val returnedAddress: Address = addresses[0]
                val strReturnedAddress = StringBuilder("")
                for (i in 0..returnedAddress.maxAddressLineIndex) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i))
                }
                strAdd = strReturnedAddress.toString()
            } else {
                Log.w(TAG, "No Address returned!")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.w(TAG, "Cannot get Address!")
        }
        return strAdd
    }
}