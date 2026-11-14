package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.AlarmEntry
import java.util.Calendar
import java.util.Locale

class AlarmScheduler(private val context: Context) {

  private val appContext = context.applicationContext
  private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

  fun syncAlarms(alarms: List<AlarmEntry>) {
    alarms.forEach { alarm ->
      if (alarm.enabled) {
        scheduleAlarm(alarm)
      } else {
        cancelAlarm(alarm.id)
      }
    }
  }

  fun scheduleAlarm(alarm: AlarmEntry) {
    val am = alarmManager ?: return
    if (alarm.id.isBlank()) return
    val triggerAtMillis = computeNextTriggerMillis(
      hour = alarm.hour.toInt(),
      minute = alarm.minute.toInt(),
      repeatDays = alarm.repeatDays.map { it.toInt() },
    )
    val pendingIntent = buildTriggerPendingIntent(
      alarmId = alarm.id,
      hour = alarm.hour.toInt(),
      minute = alarm.minute.toInt(),
      label = alarm.label,
      vibrate = alarm.vibrate,
    )
    runCatching {
      val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
      if (canExact) {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      } else {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
      }
    }
  }

  fun cancelAlarm(alarmId: String) {
    val am = alarmManager ?: return
    if (alarmId.isBlank()) return
    val pendingIntent = buildTriggerPendingIntent(alarmId, 0, 0, "", true)
    runCatching {
      am.cancel(pendingIntent)
      pendingIntent.cancel()
    }
  }

  fun scheduleSnooze(alarmId: String, label: String, vibrate: Boolean, minutes: Int = 10) {
    val am = alarmManager ?: return
    val triggerAt = System.currentTimeMillis() + minutes * 60_000L
    val cal = Calendar.getInstance().apply { timeInMillis = triggerAt }
    val pendingIntent = buildTriggerPendingIntent(
      alarmId = "${alarmId}_snooze",
      hour = cal.get(Calendar.HOUR_OF_DAY),
      minute = cal.get(Calendar.MINUTE),
      label = if (label.isBlank()) "Snoozed Alarm" else "$label (Snoozed)",
      vibrate = vibrate,
    )
    runCatching {
      val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
      if (canExact) {
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
      } else {
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
      }
    }
  }

  private fun buildTriggerPendingIntent(
    alarmId: String,
    hour: Int,
    minute: Int,
    label: String,
    vibrate: Boolean,
  ): PendingIntent {
    val intent = Intent(appContext, AlarmReceiver::class.java).apply {
      action = AlarmReceiver.ACTION_ALARM_TRIGGER
      putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
      putExtra(AlarmReceiver.EXTRA_HOUR, hour)
      putExtra(AlarmReceiver.EXTRA_MINUTE, minute)
      putExtra(AlarmReceiver.EXTRA_LABEL, label)
      putExtra(AlarmReceiver.EXTRA_VIBRATE, vibrate)
    }
    return PendingIntent.getBroadcast(
      appContext,
      alarmId.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  companion object {
    fun computeNextTriggerMillis(
      hour: Int,
      minute: Int,
      repeatDays: List<Int>,
      nowMillis: Long = System.currentTimeMillis(),
    ): Long {
      val candidate = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      if (candidate.timeInMillis <= nowMillis) {
        candidate.add(Calendar.DAY_OF_YEAR, 1)
      }
      val normalizedRepeat = repeatDays.filter { it in 1..7 }.toSet()
      if (normalizedRepeat.isEmpty()) {
        return candidate.timeInMillis
      }
      for (offset in 0..7) {
        val check = (candidate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
        val isoWeekday = (check.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
        if (isoWeekday in normalizedRepeat) {
          return check.timeInMillis
        }
      }
      return candidate.timeInMillis
    }

    fun formatCountdown(alarms: List<AlarmEntry>, nowMillis: Long = System.currentTimeMillis()): String? {
      val nextMillis = alarms
        .filter { it.enabled }
        .minOfOrNull { alarm ->
          computeNextTriggerMillis(
            hour = alarm.hour.toInt(),
            minute = alarm.minute.toInt(),
            repeatDays = alarm.repeatDays.map { it.toInt() },
            nowMillis = nowMillis,
          )
        } ?: return null

      val diffMinutes = ((nextMillis - nowMillis) / 60_000L).coerceAtLeast(0L)
      val days = diffMinutes / (60L * 24L)
      val hours = (diffMinutes % (60L * 24L)) / 60L
      val mins = diffMinutes % 60L
      return when {
        days > 0L -> String.format(Locale.getDefault(), "Next alarm in %dd %dh", days, hours)
        hours > 0L -> String.format(Locale.getDefault(), "Next alarm in %dh %dm", hours, mins)
        mins > 0L -> String.format(Locale.getDefault(), "Next alarm in %dm", mins)
        else -> "Alarm ringing in less than a minute"
      }
    }
  }
}
