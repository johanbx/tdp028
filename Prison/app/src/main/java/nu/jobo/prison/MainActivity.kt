package nu.jobo.prison

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import java.util.*
import com.firebase.ui.auth.AuthUI
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.widget.*
import android.os.Build
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : Activity(), SensorEventListener {

    companion object {
        const val TAG = "TAG_MAIN"

        const val POWER_FOR_ESCAPE = 10000
        const val SIGN_IN_AND_ASK = 120
        const val SIGN_IN_AND_EXIT = 121
        const val CHANNEL_ID = "CHANNEL_ID"
        const val REQUEST_INVITE = 555

        const val PRISONER_KEY = "PRISONER_KEY"

        const val FENCE_KEY = "FENCE_KEY"
        const val FENCE_LATITUDE: Double = 59.464997
        const val FENCE_LONGITUDE: Double = 18.048519
        const val FENCE_RADIUS_METER: Float = 100f
        const val FENCE_EXPIRATION_MILLISECONDS: Long = 200000

        const val LOCATION_PERMISSION_REQUEST_CODE = 345
    }

    // Authentication Providers (login ui)
    private var providers = Arrays.asList(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.FacebookBuilder().build())


    private var running = false
    private var sensorManager:SensorManager? = null

    lateinit var geofencingClient: GeofencingClient

    private lateinit var escapingAttemptNotificationBuilder: NotificationCompat.Builder
    private lateinit var prisonerEvents: PrisonerEvents
    private lateinit var fence: Geofence

    lateinit var mAuth: FirebaseAuth

    lateinit var stepCounter: TextView
    lateinit var statusImage: ImageView
    lateinit var pushUpCounter: TextView
    lateinit var sitUpCounter: TextView
    lateinit var powerCounter: TextView
    lateinit var userRef: String
    lateinit var prisoner: PrisonerData
    lateinit var oldPrisoner: PrisonerData

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.run {
            putSerializable(PRISONER_KEY, prisoner)
        }

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        extractActionCounterValuesFromSavedBundle(savedInstanceState)
    }

    fun restartApp() {
        finish()
        startActivity(intent)
    }

    private fun saveCurrentValuesDialog(saveCurrentValues: (Boolean) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder
            .setMessage(getString(R.string.user_already_logged_in))
            .setPositiveButton(getString(R.string.keep_new_login_button), {
                _, _ -> saveCurrentValues(true)})
            .setNegativeButton(getString(R.string.keep_old_login_button), {
                _, _ -> saveCurrentValues(false) })
            .show()
    }

    fun loginUserOnStart() {
        if (mAuth.currentUser == null){
            createAndLoginAnonymousUser()
        } else {
            initOnDatabaseChanges()
        }
    }

    private fun initOnDatabaseChanges() {
        updateUI()
        val ref = FirebaseDatabase.getInstance().getReference("users/" + mAuth.currentUser!!.uid)
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()){
                    prisoner = dataSnapshot.getValue(PrisonerData::class.java)!!
                    updateUI()
                }
            }
            override fun onCancelled(p0: DatabaseError) {}
        })
    }

    private fun updateUI() {
        if (mAuth.currentUser?.displayName.isNullOrEmpty()) {
            usernameTextView.text = getString(R.string.anonymous)
        } else {
            usernameTextView.text = mAuth.currentUser?.displayName
        }
        pushUpCounter.text = prisoner.pushUps.toString()
        sitUpCounter.text = prisoner.sitUps.toString()
        powerCounter.text =  prisoner.power.toString()
        stepCounter.text = prisoner.steps.toString()
    }

    // OBS: onActivityResult for actual logic
    // TODO: find a way to delete anonymous users after a login
    // TODO: find a way to see if an account already exist
    // (so new know if we are going to ask the users if they want to keep their
    // current prisonerData or not)
    private fun loginUser(signInIntentCode: Int) {
        var intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build()
        oldPrisoner = prisoner
        startActivityForResult(intent, signInIntentCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (data != null) {
            when (requestCode) {
                SIGN_IN_AND_EXIT -> initOnDatabaseChanges()
                SIGN_IN_AND_ASK -> {
                    saveCurrentValuesDialog(){
                        yes ->
                        if (yes) {
                            prisoner = oldPrisoner
                            dbUpdate(){
                                initOnDatabaseChanges()
                            }
                        }else {
                            initOnDatabaseChanges()
                        }
                    }
                }
            }
        }
    }

    private fun createAndLoginAnonymousUser() {
        mAuth.signInAnonymously()
            .addOnCompleteListener {
                initOnDatabaseChanges()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.unknown_login_error), Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    public override fun onStart() {
        super.onStart()
        loginUserOnStart()
        setTitle(R.string.prisoner_status_captured)
    }

    fun dbUpdate(function: () -> Unit) {
        val ref = FirebaseDatabase
                .getInstance()
                .getReference("users/" + mAuth.currentUser!!.uid)
        ref.setValue(prisoner)
    }

    // OBS: TODO: Unsafe, delete this later
    fun deleteUser(user: FirebaseUser?, callback: ()->Unit) {
        try {
            // Database
            FirebaseDatabase
                    .getInstance()
                    .getReference("users/" + user!!.uid)
                    .removeValue()

            // User
            user.delete().addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Removed user", Toast.LENGTH_SHORT).show()
                    callback()
                } else {
                    if (it.exception is FirebaseAuthRecentLoginRequiredException?) {
                        // prompt login if user cant delete itself
                        Toast.makeText(this, getString(R.string.login_to_delete_account), Toast.LENGTH_SHORT).show()
                        loginUser(SIGN_IN_AND_EXIT)
                    } else {
                        Toast.makeText(this, getString(R.string.unknown_error), Toast.LENGTH_SHORT).show()
                        Log.e(TAG, it.exception.toString())
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to delete user", Toast.LENGTH_SHORT).show()
            Log.e(TAG, e.toString())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createNotificationChannel()
        initNotification()

        extractActionCounterValuesFromSavedBundle(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        // Require Location Permission & Setup Geofence
        requirePermission(android.Manifest.permission.ACCESS_FINE_LOCATION,
                LOCATION_PERMISSION_REQUEST_CODE)
        geofencingClient = LocationServices.getGeofencingClient(this)
        initGeofence()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val buttons: Array<Button> = initButtons()

        val buttonGridView: GridView = findViewById(R.id.ButtonGridView)
        buttonGridView.adapter = ButtonAdapter(this, buttons)

        prisonerEvents = PrisonerEvents(this)

        statusImage = findViewById(R.id.image_prisoner_status)
        pushUpCounter = findViewById(R.id.pushUpsValueTextView)
        sitUpCounter = findViewById(R.id.sitUpsValueTextView)
        powerCounter = findViewById(R.id.powerValueTextView)
        stepCounter = findViewById(R.id.stepsValueTextView)

        FirebaseDynamicLinks.getInstance().getDynamicLink(intent)
                .addOnFailureListener{
                    Log.e(TAG, it.toString())}
                .addOnSuccessListener {
                    if (it != null) {
                        Toast.makeText(applicationContext,
                                "Firebase Link Detected", Toast.LENGTH_SHORT).show()
                    }
                }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?,
                                            grantResults: IntArray?) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (permissions?.isEmpty()!!){
                requirePermission(android.Manifest.permission.ACCESS_FINE_LOCATION,
                        LOCATION_PERMISSION_REQUEST_CODE)
            } else {
                for (i in 0 until permissions.size) {
                    val permission = permissions[i]
                    val grantResult = grantResults?.get(i)

                    if (permission.equals(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(applicationContext,
                                    "success: location permission granted", Toast.LENGTH_SHORT).show()
                            initGeofence()
                        } else {
                            Toast.makeText(applicationContext,
                                    "This app require location permission", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                }
            }
        }
    }

    // OBS: onRequestPermissionsResult is required.
    private fun requirePermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(
                        this,
                        permission) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    requestCode)
        }
    }

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(listOf<Geofence>(fence))
        }.build()
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    @SuppressLint("MissingPermission")
    private fun initGeofence() {
        fence = Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(FENCE_KEY)

                // Set the circular region of this geofence.
                .setCircularRegion(
                        FENCE_LATITUDE,
                        FENCE_LONGITUDE,
                        FENCE_RADIUS_METER
                )

                // Set the expiration duration of the geofence. This geofence gets automatically
                // removed after this period of time.
                .setExpirationDuration(FENCE_EXPIRATION_MILLISECONDS)


                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)

                // Create the geofence.
                .build()

        geofencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(applicationContext, "success: Geofences added", Toast.LENGTH_SHORT).show()
                // Geofences added
                // ...
            }
            addOnFailureListener {
                Toast.makeText(applicationContext, "failure: Geofences not added", Toast.LENGTH_SHORT).show()
                // Failed to add geofences
                // ...
            }
        }
    }

    private fun extractActionCounterValuesFromSavedBundle(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            prisoner = savedInstanceState.getSerializable(PRISONER_KEY) as PrisonerData
        } else {
            prisoner = PrisonerData()
        }
    }

    private fun initNotification(){
        // Create an explicit intent for an Activity in your app
        // This type of intent remembers the counter values, source:
        // https://stackoverflow.com/questions/5502427/resume-application-and-stack-from-notification
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.action = Intent.ACTION_MAIN

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        escapingAttemptNotificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Test notification")
                .setContentText("Test text")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true) // removes notification on tap
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    private fun logoutUser() {
        if (mAuth.currentUser!!.isAnonymous) {
            Toast.makeText(this, getString(R.string.cant_logout_anonymous_user_warning), Toast.LENGTH_SHORT).show()
        } else {
            AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener {
                    Toast.makeText(this, getString(R.string.user_logged_out_message), Toast.LENGTH_SHORT).show()
                    restartApp()
                }
        }
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
                R.string.button_push_up, {prisonerEvents.pushUp()})

        val sitUpButton: Button = simpleEventButton(
                R.string.button_sit_up, {prisonerEvents.sitUp()})

        val tryEscapeButton: Button = simpleEventButton(
                R.string.button_try_escape, {prisonerEvents.tryEscape()})

        /* Temporary Buttons */
        val tempCapturedButton: Button = simpleEventButton(
                "Captured", {prisonerEvents.eventCaptured()})

        val tempDiedButton: Button = simpleEventButton(
                "Died", {prisonerEvents.died()})

        val tempLogoutButton: Button = simpleEventButton(
                "Logout", {logoutUser()})

        val tempPraiseTheSunButton: Button = simpleEventButton(
                "Praise Sun", {prisonerEvents.eventPraiseTheSun()})

        val tempAdd1000PowerButton: Button = simpleEventButton(
                "+1000 Power", {prisonerEvents.tempAdd1000Power()})

        val tempGodKillButton: Button = simpleEventButton(
                "God Kill", {prisonerEvents.eventGodKill()})

        val tempWonButton: Button = simpleEventButton(
                "Win", {prisonerEvents.eventWon()})

        val tempEscapeNotificationButton: Button = simpleEventButton(
                "Notify", {escapeNotification()})

        val tempLoginButton: Button = simpleEventButton(
                "Login", {loginUser(SIGN_IN_AND_ASK)})

        val tempDeleteUserButton: Button = simpleEventButton(
                "Delete User", {
            deleteUser(mAuth.currentUser, {
                finish()
                startActivity(intent)
            })
        })

        val tempShareButton: Button = simpleEventButton(
                "Share App", {
            val shortLink = "https://prison.page.link/invite"
            val msg =  "I found this awesome game called Prison! Can you " +
                    "escape before I can? Can you beat my ${prisoner.power} power? $shortLink"
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT, msg)
            sendIntent.type = "text/plain"
            startActivity(sendIntent)
        })

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
                tempWonButton,
                tempEscapeNotificationButton,
                tempLoginButton,
                tempDeleteUserButton,
                tempShareButton)
    }

    // Temporary
    private fun escapeNotification() {
        val notificationManager = NotificationManagerCompat.from(this)
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(123, escapingAttemptNotificationBuilder.build())
    }
    // --------------

    // Source for step counter:
    // https://medium.com/@ssaurel/create-a-step-counter-fitness-app-for-android-with-kotlin-bbfb6ffe3ea7
    override fun onResume() {
        super.onResume()
        running = true
        val stepsSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

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
                prisonerEvents.step()
            }
        }
    }
}
