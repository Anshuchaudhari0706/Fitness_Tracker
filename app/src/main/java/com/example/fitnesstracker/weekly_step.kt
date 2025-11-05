package com.example.fitnesstracker

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fitnesstracker.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class WeeklyStepsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_step)

        val barChart = findViewById<BarChart>(R.id.barChart)
        val prefs = getSharedPreferences("StepData", Context.MODE_PRIVATE)

        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfLabel = SimpleDateFormat("EEE", Locale.getDefault())

        val labels = ArrayList<String>()
        val entries = ArrayList<BarEntry>()

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)

        for (i in 0..6) {
            val date = cal.time
            val key = sdfKey.format(date)
            val label = sdfLabel.format(date)
            val steps = prefs.getInt(key, 0)
            labels.add(label)
            entries.add(BarEntry(i.toFloat(), steps.toFloat()))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val ds = BarDataSet(entries, "Last 7 Days")
        val data = BarData(ds)
        barChart.data = data
        barChart.description.isEnabled = false
        barChart.axisRight.isEnabled = false
        barChart.axisLeft.setDrawGridLines(false)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.setDrawGridLines(false)
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.invalidate()
    }
}
