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
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import com.facebook.FacebookSdk;
import com.google.firebase.auth.FirebaseUser
import com.firebase.ui.auth.AuthUI
import java.util.Arrays.asList
import com.firebase.ui.auth.IdpResponse
import android.content.Intent
import android.support.annotation.NonNull
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task


class MainActivity : Activity(), SensorEventListener {

    companion object {
        const val POWER_FOR_ESCAPE = 10000
        const val RC_SIGN_IN = 123
    }

    // Authentication Providers
    var providers = Arrays.asList(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.FacebookBuilder().build())


    var running = false
    var sensorManager:SensorManager? = null

    lateinit var mAuth: FirebaseAuth
    lateinit var statusImage: ImageView
    lateinit var pushUpCounter: TextView
    lateinit var sitUpCounter: TextView
    lateinit var powerCounter: TextView
    lateinit var stepCounter: TextView

    // Source: https://stackoverflow.com/questions/45685026/how-can-i-get-a-random-number-in-kotlin
    fun ClosedRange<Int>.random() =
            Random().nextInt(endInclusive - start) +  start

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = mAuth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "Trying to Login", Toast.LENGTH_SHORT).show()
            // Create and launch sign-in intent
            startActivityForResult(
                AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .build(),
                RC_SIGN_IN)
        } else {
            Toast.makeText(this, "Already Logged in", Toast.LENGTH_SHORT).show()
            logout()
        }
    }

    fun logout() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                Toast.makeText(this, "You were logged out!", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show()
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()

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

    fun eventTryEscape(view: View) {
        val currentPower = powerCounter.text.toString().toInt()
        val powerToEscape = POWER_FOR_ESCAPE

        if (
                currentPower >= powerToEscape ||
                currentPower > (0..powerToEscape).random()) {
            prisonerEscaped()
        }
        else {
            prisonerFailedEscape()
        }
    }

    private fun prisonerFailedEscape() {
        statusImage.setImageResource(R.drawable.escape_failed)
        setTitle(R.string.prisoner_status_failed_escape)
        powerCounter.text = "0"
    }

    private fun prisonerEscaped() {
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
