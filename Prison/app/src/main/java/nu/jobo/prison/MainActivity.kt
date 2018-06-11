package nu.jobo.prison

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.util.*

class MainActivity : Activity(), SensorEventListener {

    var running = false
    var sensorManager:SensorManager? = null

    lateinit var statusImage: ImageView
    lateinit var pushUpCounter: TextView
    lateinit var sitUpCounter: TextView
    lateinit var powerCounter: TextView
    lateinit var stepCounter: TextView

    // Source: https://stackoverflow.com/questions/45685026/how-can-i-get-a-random-number-in-kotlin
    fun ClosedRange<Int>.random() =
            Random().nextInt(endInclusive - start) +  start

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        statusImage = findViewById(R.id.image_prisoner_status)
        pushUpCounter = findViewById(R.id.text_total_push_ups)
        sitUpCounter = findViewById(R.id.text_total_sit_ups)
        powerCounter = findViewById(R.id.power)
        stepCounter = findViewById(R.id.steps)
    }

    fun eventDied(view: View) {
        statusImage.setImageResource(R.drawable.died)
        setTitle(R.string.prisoner_status_died)
    }

    fun eventEscaped(view: View) {
        statusImage.setImageResource(R.drawable.escaped)
        setTitle(R.string.prisoner_status_escaping)
    }

    fun eventGodKill(view: View) {
        statusImage.setImageResource(R.drawable.godkill)
        setTitle(R.string.prisoner_status_killed_by_gods)
    }

    fun eventCaptured(view: View) {
        statusImage.setImageResource(R.drawable.bars)
        setTitle(R.string.prisoner_status_captured)
    }

    fun eventPraiseTheSun(view: View) {
        statusImage.setImageResource(R.drawable.sunpraise)
        setTitle(R.string.prisoner_status_praise_the_sun)
    }

    fun eventWon(view: View) {
        statusImage.setImageResource(R.drawable.free)
        setTitle(R.string.prisoner_status_free)
    }

    private fun powerRandomRangeIncrease(from: Int, to: Int) {
        powerCounter.text = powerCounter.text
                .toString()
                .toInt()
                .plus((from..to).random())
                .toString()
    }

    fun prisonerPushUp(view: View) {
        pushUpCounter.text = pushUpCounter.text
                .toString()
                .toInt()
                .inc()
                .toString()
        powerRandomRangeIncrease(10, 20)
    }

    fun prisonerSitUp(view: View) {
        sitUpCounter.text = sitUpCounter.text
                .toString()
                .toInt()
                .inc()
                .toString()
        powerRandomRangeIncrease(10, 20)
    }

    // Source for step counter: https://medium.com/@ssaurel/create-a-step-counter-fitness-app-for-android-with-kotlin-bbfb6ffe3ea7
    override fun onResume() {
        super.onResume()
        running = true
        var stepsSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepsSensor == null) {
            Toast.makeText(this, "No Step Counter Sensor !", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepsSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        running = false
        sensorManager?.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (running) {
            if (event != null) {
                stepCounter.text = stepCounter.text
                        .toString()
                        .toInt()
                        .inc()
                        .toString()
                powerRandomRangeIncrease(100, 200)
            }
        }
    }
}
