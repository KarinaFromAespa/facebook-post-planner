package com.facebookpostplanner.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import java.util.Calendar
import java.util.TimeZone

/** Only today and future dates are selectable in a date picker — past dates
 * are greyed out and cannot be picked. The picker works in UTC, so "today"
 * is the start of the current day in UTC. */
@OptIn(ExperimentalMaterial3Api::class)
val todayOrLaterDates = object : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val todayUtcStart = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return utcTimeMillis >= todayUtcStart
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year >= Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR)
    }
}

/** Keep a time picker from sitting on a time that has already passed. When
 * the picked day is today, the earliest schedulable time is 2 minutes from
 * now — so if the picker lands on an earlier time (e.g. the user switches
 * to AM when it is already PM), it is moved to that earliest time. On a
 * future day any time is fine. The confirm-time validation stays as the
 * backstop. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepTimeInFuture(
    timeState: TimePickerState,
    dayMillis: Long?,
    timePickerVisible: Boolean
) {
    LaunchedEffect(timeState.hour, timeState.minute, dayMillis, timePickerVisible) {
        if (!timePickerVisible) return@LaunchedEffect
        val day = dayMillis ?: return@LaunchedEffect
        val now = Calendar.getInstance()
        val picked = Calendar.getInstance().apply { timeInMillis = day }
        val isToday = now.get(Calendar.YEAR) == picked.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == picked.get(Calendar.DAY_OF_YEAR)
        if (!isToday) return@LaunchedEffect
        val cal = Calendar.getInstance().apply {
            timeInMillis = day
            set(Calendar.HOUR_OF_DAY, timeState.hour)
            set(Calendar.MINUTE, timeState.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val earliest = System.currentTimeMillis() + 2 * 60 * 1000
        if (cal.timeInMillis < earliest) {
            val e = Calendar.getInstance().apply { timeInMillis = earliest }
            timeState.hour = e.get(Calendar.HOUR_OF_DAY)
            timeState.minute = e.get(Calendar.MINUTE)
        }
    }
}
