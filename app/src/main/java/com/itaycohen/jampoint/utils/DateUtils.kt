package com.itaycohen.jampoint.utils

import android.util.Log
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.*

object DateUtils {

    private val TAG = DateUtils::class.java.simpleName

    fun utcTimeToUiLocaleTime(utcTimeStamp: String) : String? {
        val utcTime = if (utcTimeStamp.toCharArray().get(utcTimeStamp.lastIndex).toUpperCase() == 'Z')
            utcTimeStamp.substring(0 until utcTimeStamp.lastIndex)
        else
            utcTimeStamp
        val ldt = try {
            LocalDateTime.parse(utcTime)
        } catch (e: DateTimeParseException) {
            Log.w(TAG, "utcTimeToUiLocaleTime: cannot format $utcTime", e)
            return null
        }
        val zoneId = try {
            ZoneId.of(TimeZone.getDefault().id) // "for israel its "Asia/Default"
        } catch (e: DateTimeException) {
            Log.w(TAG, "utcTimeToUiLocaleTime: cannot format $utcTime", e)
            return null
        } catch (e: DateTimeException) {
            Log.w(TAG, "utcTimeToUiLocaleTime: cannot format $utcTime", e)
            return null
        }
        val zdt: ZonedDateTime = ldt.atZone(zoneId)
        return try {
            zdt.format(DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(Locale.getDefault())
            )
        } catch (e: DateTimeException) {
            Log.w(TAG, "utcTimeToUiLocaleTime: cannot format $utcTime", e)
            null
        }
    }


}