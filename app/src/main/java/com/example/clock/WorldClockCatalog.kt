package com.example.clock

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

data class WorldCityOption(
  val zoneId: String,
  val city: String,
  val region: String,
)

data class LiveWorldTime(
  val formattedTime: String,
  val amPm: String,
  val formattedDate: String,
  val offsetLabel: String,
  val abbreviation: String,
  val isDaytime: Boolean,
)

object WorldClockCatalog {

  val curatedCities: List<WorldCityOption> = listOf(
    WorldCityOption("America/New_York", "New York", "United States"),
    WorldCityOption("America/Los_Angeles", "Los Angeles", "United States"),
    WorldCityOption("America/Chicago", "Chicago", "United States"),
    WorldCityOption("America/Denver", "Denver", "United States"),
    WorldCityOption("America/Toronto", "Toronto", "Canada"),
    WorldCityOption("America/Vancouver", "Vancouver", "Canada"),
    WorldCityOption("America/Mexico_City", "Mexico City", "Mexico"),
    WorldCityOption("America/Sao_Paulo", "São Paulo", "Brazil"),
    WorldCityOption("America/Argentina/Buenos_Aires", "Buenos Aires", "Argentina"),
    WorldCityOption("Europe/London", "London", "United Kingdom"),
    WorldCityOption("Europe/Dublin", "Dublin", "Ireland"),
    WorldCityOption("Europe/Paris", "Paris", "France"),
    WorldCityOption("Europe/Berlin", "Berlin", "Germany"),
    WorldCityOption("Europe/Madrid", "Madrid", "Spain"),
    WorldCityOption("Europe/Rome", "Rome", "Italy"),
    WorldCityOption("Europe/Zurich", "Zurich", "Switzerland"),
    WorldCityOption("Europe/Amsterdam", "Amsterdam", "Netherlands"),
    WorldCityOption("Europe/Stockholm", "Stockholm", "Sweden"),
    WorldCityOption("Europe/Athens", "Athens", "Greece"),
    WorldCityOption("Europe/Istanbul", "Istanbul", "Türkiye"),
    WorldCityOption("Africa/Cairo", "Cairo", "Egypt"),
    WorldCityOption("Africa/Johannesburg", "Johannesburg", "South Africa"),
    WorldCityOption("Africa/Lagos", "Lagos", "Nigeria"),
    WorldCityOption("Africa/Nairobi", "Nairobi", "Kenya"),
    WorldCityOption("Asia/Dubai", "Dubai", "UAE"),
    WorldCityOption("Asia/Riyadh", "Riyadh", "Saudi Arabia"),
    WorldCityOption("Asia/Kolkata", "Mumbai", "India"),
    WorldCityOption("Asia/Bangkok", "Bangkok", "Thailand"),
    WorldCityOption("Asia/Jakarta", "Jakarta", "Indonesia"),
    WorldCityOption("Asia/Singapore", "Singapore", "Singapore"),
    WorldCityOption("Asia/Hong_Kong", "Hong Kong", "China"),
    WorldCityOption("Asia/Shanghai", "Shanghai", "China"),
    WorldCityOption("Asia/Taipei", "Taipei", "Taiwan"),
    WorldCityOption("Asia/Seoul", "Seoul", "South Korea"),
    WorldCityOption("Asia/Tokyo", "Tokyo", "Japan"),
    WorldCityOption("Australia/Perth", "Perth", "Australia"),
    WorldCityOption("Australia/Sydney", "Sydney", "Australia"),
    WorldCityOption("Pacific/Auckland", "Auckland", "New Zealand"),
    WorldCityOption("Pacific/Honolulu", "Honolulu", "United States"),
    WorldCityOption("UTC", "UTC", "Coordinated Universal Time"),
  )

  fun formatZoneTime(
    zoneId: String,
    nowMillis: Long,
    use24Hour: Boolean,
    localTimeZone: TimeZone = TimeZone.getDefault(),
  ): LiveWorldTime {
    val targetTz = TimeZone.getTimeZone(zoneId)
    val nowDate = Date(nowMillis)

    val timePattern = if (use24Hour) "HH:mm" else "h:mm"
    val timeFmt = SimpleDateFormat(timePattern, Locale.getDefault()).apply { timeZone = targetTz }
    val amPmFmt = SimpleDateFormat("a", Locale.getDefault()).apply { timeZone = targetTz }
    val dateFmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).apply { timeZone = targetTz }
    val abbrFmt = SimpleDateFormat("zzz", Locale.getDefault()).apply { timeZone = targetTz }

    val targetCal = Calendar.getInstance(targetTz).apply { timeInMillis = nowMillis }
    val localCal = Calendar.getInstance(localTimeZone).apply { timeInMillis = nowMillis }
    val hour = targetCal.get(Calendar.HOUR_OF_DAY)

    val offsetDiffMinutes =
      (targetTz.getOffset(nowMillis) - localTimeZone.getOffset(nowMillis)) / 60000
    val dayRelation = relativeDayLabel(localCal, targetCal)
    val diffText = formatOffsetDifference(offsetDiffMinutes)
    val offsetLabel = "$dayRelation • $diffText"

    return LiveWorldTime(
      formattedTime = timeFmt.format(nowDate),
      amPm = if (use24Hour) "" else amPmFmt.format(nowDate),
      formattedDate = dateFmt.format(nowDate),
      offsetLabel = offsetLabel,
      abbreviation = abbrFmt.format(nowDate),
      isDaytime = hour in 6..18,
    )
  }

  fun formatHeroTime(
    nowMillis: Long,
    use24Hour: Boolean,
  ): Triple<String, String, String> {
    val nowDate = Date(nowMillis)
    val tz = TimeZone.getDefault()
    val timePattern = if (use24Hour) "HH:mm:ss" else "h:mm:ss"
    val timeFmt = SimpleDateFormat(timePattern, Locale.getDefault()).apply { timeZone = tz }
    val amPmFmt = SimpleDateFormat("a", Locale.getDefault()).apply { timeZone = tz }
    val dateFmt = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).apply { timeZone = tz }
    return Triple(
      timeFmt.format(nowDate),
      if (use24Hour) "" else amPmFmt.format(nowDate),
      dateFmt.format(nowDate),
    )
  }

  private fun relativeDayLabel(localCal: Calendar, targetCal: Calendar): String {
    val localKey = localCal.get(Calendar.YEAR) * 1000 + localCal.get(Calendar.DAY_OF_YEAR)
    val targetKey = targetCal.get(Calendar.YEAR) * 1000 + targetCal.get(Calendar.DAY_OF_YEAR)
    return when {
      targetKey == localKey -> "Today"
      targetKey > localKey -> "Tomorrow"
      else -> "Yesterday"
    }
  }

  private fun formatOffsetDifference(diffMinutes: Int): String {
    if (diffMinutes == 0) return "Same time"
    val sign = if (diffMinutes > 0) "+" else "−"
    val absMin = abs(diffMinutes)
    val hours = absMin / 60
    val mins = absMin % 60
    return when {
      mins == 0 -> "$sign${hours}h"
      hours == 0 -> "$sign${mins}m"
      else -> "$sign${hours}h ${mins}m"
    }
  }
}
