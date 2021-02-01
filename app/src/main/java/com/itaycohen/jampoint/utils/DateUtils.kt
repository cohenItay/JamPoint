package com.itaycohen.jampoint.utils

import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.*

object DateUtils {

    fun utcTimeToUiLocaleTime(utcTimeStamp: String) : String? {
        val ldt = try {
            LocalDateTime.parse(utcTimeStamp)
        } catch (e: DateTimeParseException) {
            return null
        }
        val zoneId = try {
            ZoneId.of(TimeZone.getDefault().id) // "for israel its "Asia/Default"
        } catch (e: DateTimeException) {
            return null
        } catch (e: DateTimeException) {
            return null
        }
        val zdt: ZonedDateTime = ldt.atZone(zoneId)
        return try {
            zdt.format(DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.LONG)
                .withLocale(Locale.getDefault())
            )
        } catch (e: DateTimeException) {
            null
        }
    }


}