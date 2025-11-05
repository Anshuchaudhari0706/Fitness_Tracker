package com.example.fitnesstracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null

    private lateinit var tvSteps: TextView
    private lateinit var tvCalories: TextView
    private lateinit var tvDistance: TextView
    private lateinit var progressGoal: ProgressBar
    private lateinit var switchBackground: Switch

    private lateinit var prefs: android.content.SharedPreferences
    private var previousTotalSteps = 0f
    private var lastSavedDate = ""
    private val stepGoal = 10000
    private val caloriesPerStep = 0.04
    private val stepLengthMeters = 0.78

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSteps = findViewById(R.id.tvSteps)
        tvCalories = findViewById(R.id.tvCalories)
        tvDistance = findViewById(R.id.tvDistance)
        progressGoal = findViewById(R.id.progressGoal)
        switchBackground = findViewById(R.id.switchBackground)

        val btnWeekly = findViewById<Button>(R.id.btnWeeklySteps)
        val btnMonthly = findViewById<Button>(R.id.btnMonthlySteps)
        val tvBMI = findViewById<TextView>(R.id.tvBMI)
        val etHeight = findViewById<EditText>(R.id.etHeight)
        val etWeight = findViewById<EditText>(R.id.etWeight)
        val btnCalculateBMI = findViewById<Button>(R.id.btnCalculateBMI)

        ensureRecognitionPermission()
        ensurePostNotificationsPermissionIfNeeded()

        prefs = getSharedPreferences("StepData", Context.MODE_PRIVATE)
        previousTotalSteps = prefs.getFloat("previousTotalSteps", 0f)
        lastSavedDate = prefs.getString("lastDate", "") ?: ""
        dailyResetIfNeeded()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        btnCalculateBMI.setOnClickListener {
            val h = etHeight.text.toString().toFloatOrNull()
            val w = etWeight.text.toString().toFloatOrNull()
            if (h != null && w != null && h > 0) {
                val bmi = w / (h / 100).pow(2)
                val category = when {
                    bmi < 18.5 -> "Underweight"
                    bmi < 24.9 -> "Normal"
                    bmi < 29.9 -> "Overweight"
                    else -> "Obese"
                }
                tvBMI.text = "BMI: %.2f (%s)".format(bmi, category)
            }
        }

        switchBackground.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ContextCompat.startForegroundService(this, Intent(this, StepService::class.java))
            } else {
                stopService(Intent(this, StepService::class.java))
            }
        }

        btnWeekly.setOnClickListener { startActivity(Intent(this, WeeklyStepsActivity::class.java)) }
        btnMonthly.setOnClickListener { startActivity(Intent(this, MonthlyStepsActivity::class.java)) }

        updateUiFromStored()
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.also { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        updateUiFromStored()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSinceBoot = event.values[0]
            val currentSteps = (totalSinceBoot - previousTotalSteps).coerceAtLeast(0f)
            persistAndUpdate(currentSteps.toInt())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun dailyResetIfNeeded() {
        val today = todayDate()
        if (today != lastSavedDate) {
            previousTotalSteps = 0f
            prefs.edit().putFloat("previousTotalSteps", 0f).putString("lastDate", today).apply()
            persistAndUpdate(0)
        }
    }

    private fun persistAndUpdate(steps: Int) {
        val dayLabel = todayShort()
        val dateKey = todayDate()
        prefs.edit().putInt(dayLabel, steps).putInt(dateKey, steps).putString("lastDate", dateKey).apply()
        updateUi(steps)
    }

    private fun updateUiFromStored() {
        val stepsToday = prefs.getInt(todayDate(), 0)
        updateUi(stepsToday)
    }

    private fun updateUi(steps: Int) {
        val calories = steps * caloriesPerStep
        val distanceKm = (steps * stepLengthMeters) / 1000.0
        tvSteps.text = "Steps: $steps / $stepGoal"
        progressGoal.progress = steps.coerceAtMost(stepGoal)
        tvCalories.text = "Calories: %.1f".format(calories)
        tvDistance.text = "Distance: %.2f km".format(distanceKm)
    }

    private fun todayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun todayShort(): String =
        SimpleDateFormat("EEE", Locale.getDefault()).format(Date())

    private fun ensureRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 1001)
            }
        }
    }

    private fun ensurePostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1002)
            }
        }
    }
}
