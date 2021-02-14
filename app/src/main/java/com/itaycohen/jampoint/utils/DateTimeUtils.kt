package com.itaycohen.jampoint.utils

import android.content.Context
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.itaycohen.jampoint.R
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.FormatStyle
import java.util.*

object DateTimeUtils {

    private val TAG = DateTimeUtils::class.java.simpleName

    fun utcTimeToUiLocaleTime(utcTimeStamp: String) : String? {
        val utcTime = if (utcTimeStamp.toCharArray()[utcTimeStamp.lastIndex].toUpperCase() == 'Z')
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
        }
        val zdt: ZonedDateTime = ldt.atZone(zoneId)
        return try {
            zdt.format(
                DateTimeFormatter
                    .ofLocalizedDateTime(FormatStyle.MEDIUM)
                    .withLocale(Locale.getDefault())
            )
        } catch (e: DateTimeException) {
            Log.w(TAG, "utcTimeToUiLocaleTime: cannot format $utcTime", e)
            null
        }
    }

    fun getClearedUtc(): Calendar {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utc.clear()
        return utc
    }

    object PickDateTimeHelper {

        fun launch(
            ctx: Context,
            childFragmentManager: FragmentManager,
            timeStampCallback: (String) -> Unit
        ) {
            val typedValue = TypedValue()
            val dialogTheme = if (ctx.theme.resolveAttribute(
                    R.attr.materialCalendarTheme,
                    typedValue,
                    true
                )) {
                typedValue.data
            } else {
                return
            }
            val calendar = DateTimeUtils.getClearedUtc()
            val today = MaterialDatePicker.todayInUtcMilliseconds()
            calendar.timeInMillis = today
            calendar.roll(Calendar.MONTH, 2)
            val nextTwoMonths = calendar.timeInMillis
            val constraints = CalendarConstraints.Builder()
                .setStart(today)
                .setEnd(nextTwoMonths)
                .setOpenAt(today)
                .setValidator(DateValidatorPointForward.now())
                .build()
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTheme(dialogTheme)
                .setSelection(today)
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .setTitleText(R.string.define_meet_time)
                .setCalendarConstraints(constraints)
                .build()
            picker.addOnPositiveButtonClickListener { timeInMillis ->
                showTimePicker(childFragmentManager, timeInMillis, timeStampCallback)
            }
            picker.show(childFragmentManager, picker.toString())
        }

        private fun showTimePicker(
            childFragmentManager: FragmentManager,
            timeForDateInMillis: Long,
            timeStampCallback: (String) -> Unit
        ) {
            val materialTimePicker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .build()
            materialTimePicker.addOnPositiveButtonClickListener {
                val utcTimeStamp = with(DateTimeUtils.getClearedUtc()) {
                    isLenient = false
                    timeInMillis = timeForDateInMillis
                    roll(Calendar.HOUR_OF_DAY, materialTimePicker.hour)
                    roll(Calendar.MINUTE, materialTimePicker.minute)
                    toInstant().toString()
                }
                timeStampCallback(utcTimeStamp)
            }
            materialTimePicker.show(childFragmentManager, materialTimePicker.toString())
        }
    }
}