package com.phoneguard.ui.util

import java.text.DateFormat
import java.util.Date

/** Formats a timestamp as a short date and time in the user's locale, e.g. "4 Oct, 4:30 pm". */
fun formatDateTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))

/** Formats a timestamp as a short date in the user's locale. */
fun formatDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
