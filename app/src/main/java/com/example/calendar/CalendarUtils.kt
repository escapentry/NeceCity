package com.example.calendar

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CalendarDayCell(
  val dateIso: String,
  val dayOfMonth: Int,
  val inCurrentMonth: Boolean,
  val isToday: Boolean,
)

object CalendarUtils {

  private fun isoFormatter(): SimpleDateFormat =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }

  fun todayIso(): String = isoFormatter().format(Calendar.getInstance().time)

  fun currentYearMonth(): Pair<Int, Int> {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
  }

  fun monthTitle(year: Int, monthZeroBased: Int): String {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, monthZeroBased)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
  }

  fun formatSelectedDateHeading(dateIso: String): String {
    val parsed = runCatching { isoFormatter().parse(dateIso) }.getOrNull() ?: return dateIso
    return SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(parsed)
  }

  fun shiftMonth(year: Int, monthZeroBased: Int, deltaMonths: Int): Pair<Int, Int> {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, monthZeroBased)
      set(Calendar.DAY_OF_MONTH, 1)
      add(Calendar.MONTH, deltaMonths)
    }
    return cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
  }

  fun buildMonthGrid(year: Int, monthZeroBased: Int): List<CalendarDayCell> {
    val today = todayIso()
    val firstOfMonth = Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, monthZeroBased)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    val leadingDays = firstOfMonth.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val cursor = (firstOfMonth.clone() as Calendar).apply {
      add(Calendar.DAY_OF_MONTH, -leadingDays)
    }

    val daysInMonth = firstOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val totalCells = if (leadingDays + daysInMonth > 35) 42 else 35

    val fmt = isoFormatter()
    return List(totalCells) {
      val cellIso = fmt.format(cursor.time)
      val cell = CalendarDayCell(
        dateIso = cellIso,
        dayOfMonth = cursor.get(Calendar.DAY_OF_MONTH),
        inCurrentMonth = cursor.get(Calendar.MONTH) == monthZeroBased && cursor.get(Calendar.YEAR) == year,
        isToday = cellIso == today,
      )
      cursor.add(Calendar.DAY_OF_MONTH, 1)
      cell
    }
  }

  fun formatTimeRange(startMinutes: Long, endMinutes: Long): String {
    if (startMinutes < 0L) return "All day"
    val startStr = formatMinuteOfDay(startMinutes.toInt())
    if (endMinutes < 0L || endMinutes == startMinutes) return startStr
    return "$startStr – ${formatMinuteOfDay(endMinutes.toInt())}"
  }

  fun formatMinuteOfDay(minuteOfDay: Int): String {
    val safe = minuteOfDay.coerceIn(0, 1439)
    val hour24 = safe / 60
    val minute = safe % 60
    val amPm = if (hour24 < 12) "AM" else "PM"
    val hour12 = when (val h = hour24 % 12) {
      0 -> 12
      else -> h
    }
    return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm)
  }
}
