package com.example.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val alarmId = intent.getStringExtra(EXTRA_ALARM_ID) ?: "nececity_alarm"
    val notifId = alarmId.hashCode()
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    when (intent.action) {
      ACTION_ALARM_DISMISS -> {
        nm.cancel(notifId)
      }
      ACTION_ALARM_SNOOZE -> {
        nm.cancel(notifId)
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        AlarmScheduler(context).scheduleSnooze(alarmId, label, vibrate)
      }
      ACTION_ALARM_TRIGGER -> {
        ensureAlarmChannel(context)
        val hour = intent.getIntExtra(EXTRA_HOUR, 7)
        val minute = intent.getIntExtra(EXTRA_MINUTE, 0)
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        val notif = buildAlarmNotification(
          context = context,
          alarmId = alarmId,
          hour = hour,
          minute = minute,
          label = label,
          vibrate = vibrate,
        )
        runCatching { nm.notify(notifId, notif) }
      }
    }
  }

  companion object {
    const val ALARM_CHANNEL_ID = "nececity_alarms"
    const val ACTION_ALARM_TRIGGER = "com.thallyace.nececity.ACTION_ALARM_TRIGGER"
    const val ACTION_ALARM_DISMISS = "com.thallyace.nececity.ACTION_ALARM_DISMISS"
    const val ACTION_ALARM_SNOOZE = "com.thallyace.nececity.ACTION_ALARM_SNOOZE"
    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_HOUR = "extra_hour"
    const val EXTRA_MINUTE = "extra_minute"
    const val EXTRA_LABEL = "extra_label"
    const val EXTRA_VIBRATE = "extra_vibrate"

    fun ensureAlarmChannel(context: Context) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
      if (nm.getNotificationChannel(ALARM_CHANNEL_ID) != null) return

      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val channel = NotificationChannel(
        ALARM_CHANNEL_ID,
        "NeceCity Alarms",
        NotificationManager.IMPORTANCE_HIGH,
      ).apply {
        description = "Scheduled alarm notifications"
        enableVibration(true)
        setSound(
          soundUri,
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build(),
        )
      }
      nm.createNotificationChannel(channel)
    }

    fun buildAlarmNotification(
      context: Context,
      alarmId: String,
      hour: Int,
      minute: Int,
      label: String,
      vibrate: Boolean,
    ): Notification {
      val amPm = if (hour < 12) "AM" else "PM"
      val h12 = if (hour % 12 == 0) 12 else hour % 12
      val timeText = String.format(Locale.getDefault(), "%d:%02d %s", h12, minute, amPm)
      val title = if (label.isNotBlank()) label else "NeceCity Alarm"

      val openAppIntent = PendingIntent.getActivity(
        context,
        alarmId.hashCode(),
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

      val dismissIntent = PendingIntent.getBroadcast(
        context,
        (alarmId + "_dismiss").hashCode(),
        Intent(context, AlarmReceiver::class.java).apply {
          action = ACTION_ALARM_DISMISS
          putExtra(EXTRA_ALARM_ID, alarmId)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

      val snoozeIntent = PendingIntent.getBroadcast(
        context,
        (alarmId + "_snooze_btn").hashCode(),
        Intent(context, AlarmReceiver::class.java).apply {
          action = ACTION_ALARM_SNOOZE
          putExtra(EXTRA_ALARM_ID, alarmId)
          putExtra(EXTRA_LABEL, label)
          putExtra(EXTRA_VIBRATE, vibrate)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )

      return NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(timeText)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setAutoCancel(true)
        .setContentIntent(openAppIntent)
        .addAction(0, "Dismiss", dismissIntent)
        .addAction(0, "Snooze 10m", snoozeIntent)
        .build()
    }
  }
}
