package com.example.fitnesstracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class StepService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var prefs: android.content.SharedPreferences
    private var previousTotalSteps = 0f
    private val channelId = "steps_channel"
    private val notificationId = 101
    private val caloriesPerStep = 0.04
    private val stepLengthMeters = 0.78

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("StepData", Context.MODE_PRIVATE)
        previousTotalSteps = prefs.getFloat("previousTotalSteps", 0f)
        createChannel()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        startForeground(notificationId, buildNotification(0, 0.0, 0.0))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stepSensor?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val total = event.values[0]
            val steps = (total - previousTotalSteps).coerceAtLeast(0f).toInt()
            saveSteps(steps)
            val calories = steps * caloriesPerStep
            val distanceKm = (steps * stepLengthMeters) / 1000.0
            val n = buildNotification(steps, calories, distanceKm)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(notificationId, n)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun saveSteps(steps: Int) {
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val dayLabel = SimpleDateFormat("EEE", Locale.getDefault()).format(Date())
        prefs.edit().putInt(dateKey, steps).putInt(dayLabel, steps).putString("lastDate", dateKey).apply()
    }

    private fun buildNotification(steps: Int, calories: Double, distanceKm: Double): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Steps: $steps")
            .setContentText("Calories: %.1f  •  Distance: %.2f km".format(calories, distanceKm))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Step Tracking", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
