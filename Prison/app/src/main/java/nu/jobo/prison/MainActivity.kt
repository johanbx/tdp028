package nu.jobo.prison

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import android.content.Intent
import android.widget.*


class MainActivity : Activity(), SensorEventListener {

    companion object {
        const val POWER_FOR_ESCAPE = 10000
        const val RC_SIGN_IN = 123
    }

    // Authentication Providers (login ui)
    private var providers = Arrays.asList(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.FacebookBuilder().build())


    private var running = false
    private var sensorManager:SensorManager? = null

    private lateinit var prisonerEvents: PrisonerEvents
    private lateinit var mAuth: FirebaseAuth
    private lateinit var stepCounter: TextView

    lateinit var statusImage: ImageView
    lateinit var pushUpCounter: TextView
    lateinit var sitUpCounter: TextView
    lateinit var powerCounter: TextView

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
        }

        // TODO: Fetch saved data from user in the database and fill out the fields

        setTitle(R.string.prisoner_status_captured)
    }

    private fun logout() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                Toast.makeText(this, "You were logged out!", Toast.LENGTH_SHORT).show()
                finish()
                startActivity(intent)
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val currentUser = FirebaseAuth.getInstance().currentUser
                Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show()
                // TODO: Load user
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

        val buttons: Array<Button> = initButtons()

        val buttonGridView: GridView = findViewById(R.id.ButtonGridView)
        buttonGridView.adapter = ButtonAdapter(this, buttons)

        statusImage = findViewById(R.id.image_prisoner_status)
        pushUpCounter = findViewById(R.id.text_total_push_ups)
        sitUpCounter = findViewById(R.id.text_total_sit_ups)
        powerCounter = findViewById(R.id.power)
        stepCounter = findViewById(R.id.steps)

        prisonerEvents = PrisonerEvents(this)
    }

    private fun simpleEventButton(resourceId: Int, function: () -> Unit): Button {
        return simpleEventButton(resources.getString(resourceId), function)
    }

    private fun simpleEventButton(text: String, function: () -> Unit): Button {
        val button = Button(this)
        button.text = text
        button.setOnClickListener {
            function()
        }
        return button
    }

    private fun initButtons(): Array<Button> {

        val pushUpButton: Button = simpleEventButton(
                R.string.button_push_up, {prisonerEvents.prisonerPushUp()})

        val sitUpButton: Button = simpleEventButton(
                R.string.button_sit_up, {prisonerEvents.prisonerSitUp()})

        val tryEscapeButton: Button = simpleEventButton(
                R.string.button_try_escape, {prisonerEvents.tryEscape()})

        /* Temporary Buttons */
        val tempCapturedButton: Button = simpleEventButton(
                "Captured", {prisonerEvents.eventCaptured()})

        val tempDiedButton: Button = simpleEventButton(
                "Died", {prisonerEvents.died()})

        val tempLogoutButton: Button = simpleEventButton(
                "Logout", {logout()})

        val tempPraiseTheSunButton: Button = simpleEventButton(
                "Praise Sun", {prisonerEvents.eventPraiseTheSun()})

        val tempAdd1000PowerButton: Button = simpleEventButton(
                "+1000 Power", {prisonerEvents.tempAdd1000Power()})

        val tempGodKillButton: Button = simpleEventButton(
                "God Kill", {prisonerEvents.eventGodKill()})

        val tempWonButton: Button = simpleEventButton(
                "Win", {prisonerEvents.eventWon()})

        return arrayOf<Button>(
                pushUpButton,
                sitUpButton,
                tempCapturedButton,
                tempDiedButton,
                tempLogoutButton,
                tempPraiseTheSunButton,
                tempAdd1000PowerButton,
                tempGodKillButton,
                tryEscapeButton,
                tempWonButton)
    }

    // Temporary
    fun tempLogout() {
        logout()
    }
    // --------------

    // Source for step counter: https://medium.com/@ssaurel/create-a-step-counter-fitness-app-for-android-with-kotlin-bbfb6ffe3ea7
    override fun onResume() {
        super.onResume()
        running = true
        val stepsSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

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
                prisonerEvents.powerFromStep()
            }
        }
    }
}
