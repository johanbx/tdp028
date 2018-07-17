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
import android.content.res.Configuration
import android.graphics.Color
import android.media.MediaPlayer
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.View
import com.google.android.gms.location.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : Activity(), SensorEventListener {

    companion object {
        const val TAG = "MY_MAINACTIVITY"

        const val POWER_FOR_ESCAPE = 10000
        const val SIGN_IN_AND_ASK = 120
        const val SIGN_IN_AND_EXIT = 121
        const val CHANNEL_ID = "CHANNEL_ID"
        const val FRIEND_INVITE_POWER_BONUS = "FRIEND_INVITE_POWER_BONUS"
        const val FIRST_TIME_RUN = "FIRST_TIME_RUN"

        const val FENCE_KEY = "FENCE_KEY"
        const val FENCE_RADIUS_METER: Float = 100f
        const val FENCE_EXPIRATION_MILLISECONDS: Long = 200000

        const val LOCATION_PERMISSION_REQUEST_CODE = 345

        const val INTENT_LOGIN = "INTENT_LOGIN"
        const val INTENT_LOGOUT = "INTENT_LOGOUT"
        const val INTENT_DELETE_ACCOUNT = "INTENT_DELETE_ACCOUNT"
        const val INTENT_WON_GAME = "INTENT_WON_GAME"

        const val PRISONER_POWER = "PRISONER_POWER"

        const val MUSIC_POSITION = "MUSIC_POSITION"

        lateinit var mediaPlayer: MediaPlayer
        var mediaPlayerMuted = false

        var escaped = false
    }

    // Authentication Providers (login ui)
    private var providers = Arrays.asList(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.FacebookBuilder().build())


    private var running = false
    private var sensorManager:SensorManager? = null
    private var firstTimeRun = false

    var prisoner = PrisonerData()

    lateinit var geofencingClient: GeofencingClient

    private lateinit var escapingAttemptNotificationBuilder: NotificationCompat.Builder
    private lateinit var prisonerEvents: PrisonerEvents
    private lateinit var fence: Geofence
    private lateinit var remoteConfig: FirebaseRemoteConfig

    private var fenceLatitude: Double = 0.0
    private var fenceLongitude: Double = 0.0

    lateinit var mAuth: FirebaseAuth
    lateinit var mFirebaseAnalytics: FirebaseAnalytics
    lateinit var mFusedLocationClient: FusedLocationProviderClient
    lateinit var analyticEvents: AnalyticEvents

    lateinit var stepCounter: TextView
    lateinit var statusImage: ImageView
    lateinit var pushUpCounter: TextView
    lateinit var sitUpCounter: TextView
    lateinit var powerCounter: TextView
    lateinit var oldPrisoner: PrisonerData

    /* "On" events */
    public override fun onStart() {
        super.onStart()
        checkFirstTimeRun()
    }

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate called")

        if (firstTimeRun) { return startWelcomeActivity() }
        setContentView(R.layout.activity_main)

        initRemoteConfig()
        createNotificationChannel()
        initNotification()

        mAuth = FirebaseAuth.getInstance()
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Require Location Permission & Setup Geofence
        requirePermission(android.Manifest.permission.ACCESS_FINE_LOCATION,
                LOCATION_PERMISSION_REQUEST_CODE)
        geofencingClient = LocationServices.getGeofencingClient(this)
        mFusedLocationClient.lastLocation.addOnCompleteListener {
            if (it.isSuccessful) {
                fenceLatitude = it.result.latitude
                fenceLongitude = it.result.longitude
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val buttons = initButtons()
        ButtonGridView.adapter = ButtonAdapter(this, buttons)

        prisonerEvents = PrisonerEvents(this)
        analyticEvents = AnalyticEvents(mFirebaseAnalytics)

        statusImage = findViewById(R.id.image_prisoner_status)
        pushUpCounter = findViewById(R.id.pushUpsValueTextView)
        sitUpCounter = findViewById(R.id.sitUpsValueTextView)
        powerCounter = findViewById(R.id.powerValueTextView)
        stepCounter = findViewById(R.id.stepsValueTextView)

        if (prisoner.power == 0 && intent.getIntExtra(FRIEND_INVITE_POWER_BONUS, 0) != 0) {
            prisoner.power = intent.getIntExtra(FRIEND_INVITE_POWER_BONUS, 2000)
            Toast.makeText(applicationContext,
                    getString(R.string.given_free_power), Toast.LENGTH_LONG).show()
            analyticEvents.wasInvited(mAuth.currentUser!!.uid)
        }

        initMediaPlayer(savedInstanceState?.getInt(MUSIC_POSITION, 0)?: 0)

        applyTheme()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.run {
            putInt(MUSIC_POSITION, mediaPlayer.currentPosition)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.setLocale(this)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(newBase!!))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d(TAG, "OnActivityResult called")

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
                        }
                    }
                }
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

    override fun onNewIntent(intent: Intent?) {
        if (intent != null)
            setIntent(intent)
    }

    // Source for step counter:
    // https://medium.com/@ssaurel/create-a-step-counter-fitness-app-for-android-with-kotlin-bbfb6ffe3ea7
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")

        /* Media player */
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }

        /* Steps */
        running = true
        val stepsSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (stepsSensor == null) {
            Toast.makeText(this, "No Step Counter Sensor !", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepsSensor, SensorManager.SENSOR_DELAY_UI)
        }

        /* Intents */
        if (intent.getStringExtra(INTENT_LOGIN) == INTENT_LOGIN) {
            intent.removeExtra(INTENT_LOGIN)
            loginUser(SIGN_IN_AND_ASK)
        } else if (intent.getStringExtra(INTENT_DELETE_ACCOUNT) == INTENT_DELETE_ACCOUNT){
            intent.removeExtra(INTENT_DELETE_ACCOUNT)
        } else if (intent.getStringExtra(INTENT_WON_GAME) == INTENT_WON_GAME) {
            Log.d(TAG, "User won the game")
            intent.removeExtra(INTENT_WON_GAME)
            prisonerEvents.eventWon()
            analyticEvents.gameWon(mAuth.currentUser!!.uid, prisoner.power)
        }
    }

    override fun onPause() {
        super.onPause()
        running = false
        sensorManager?.unregisterListener(this)
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer.pause()
        Log.d(TAG, "onStop called")
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
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

    /* Init:s */
    private fun initMediaPlayer(musicPosition: Int = 0) {
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.background_music)
        mediaPlayer.seekTo(musicPosition)
        mediaPlayer.isLooping = true
        mediaPlayer.start()
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
                .setContentTitle(getString(R.string.successful_escape))
                .setContentText(getString(R.string.on_escape_instructions))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true) // removes notification on tap
    }

    @SuppressLint("MissingPermission")
    fun initGeofence() {
        fence = Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(FENCE_KEY)

                // Set the circular region of this geofence.
                .setCircularRegion(
                        fenceLatitude,
                        fenceLongitude,
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

    private fun initRemoteConfig() {
        remoteConfig = FirebaseRemoteConfig.getInstance()
        remoteConfig.setConfigSettings(
                FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(BuildConfig.DEBUG)
                        .build())
        remoteConfig.setDefaults(R.xml.remote_config_defaults)
        remoteConfig.fetch(0).addOnCompleteListener {
            if (it.isSuccessful) {
                remoteConfig.activateFetched()
                if (!remoteConfig.getString("dev_welcome_message").isNullOrEmpty()) {
                    devMessageTextView.visibility = View.VISIBLE
                    devMessageTextView.text = remoteConfig.getString("dev_welcome_message")
                }
            } else {
                Toast.makeText(this, "failed to fetch remote config", Toast.LENGTH_LONG).show()
            }
        }
    }

    /* Authentication Related */
    private fun logoutUser() {
        when (mAuth.currentUser){
            null -> {
                Log.e(TAG, "Tried to logout a non existing user")
                return
            }
        }

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

    private fun saveCurrentValuesDialog(saveCurrentValues: (Boolean) -> Unit) {
        val builder = AlertDialog.Builder(this)
        builder
                .setMessage("Cloud Save Detected. Which data do you want to keep?")
                .setPositiveButton("This new login", {
                    _, _ -> saveCurrentValues(true)})
                .setNegativeButton("Old login", {
                    _, _ -> saveCurrentValues(false) })
                .show()
    }

    private fun loginUserOnStart() {
        if (mAuth.currentUser == null){
            createAndLoginAnonymousUser()
        } else {
            initOnDatabaseChanges()
        }
    }

    // OBS: onActivityResult for actual logic
    // TODO: find a way to delete anonymous users after a login
    // TODO: find a way to see if an account already exist
    // (so new know if we are going to ask the users if they want to keep their
    // current prisonerData or not)
    private fun loginUser(signInIntentCode: Int) {
        if (mAuth.currentUser!!.isAnonymous) {
            var intent = AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .build()
            oldPrisoner = prisoner
            startActivityForResult(intent, signInIntentCode)
        } else {
            Toast.makeText(this, getString(R.string.already_logged_in_message),
                    Toast.LENGTH_SHORT).show()
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

    /* Misc */
    private fun restartApp() {
        finish()
        val restartActivity = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        mediaPlayer.release()
        startActivity(restartActivity)
    }

    private fun startWelcomeActivity() {
        val intent = Intent(applicationContext, WelcomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun checkFirstTimeRun() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        firstTimeRun = prefs.getBoolean(FIRST_TIME_RUN, true)
        if (firstTimeRun) {
            prefs.edit().putBoolean(FIRST_TIME_RUN, false).commit()
            return startWelcomeActivity()
        } else {
            loginUserOnStart()
            setTitle(R.string.prisoner_status_captured)
        }
    }

    fun dbUpdate(function: () -> Unit) {
        if (mAuth.currentUser != null) {
            val ref = FirebaseDatabase
                    .getInstance()
                    .getReference("users/" + mAuth.currentUser!!.uid)
            ref.setValue(prisoner)
            function()
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

    // Temporary
    fun escapeNotification() {
        val notificationManager = NotificationManagerCompat.from(this)
        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(123, escapingAttemptNotificationBuilder.build())
    }

    /* UI-related */
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

    // Used in Remote Configuration
    private fun applyTheme() {
        powerTextView.setTextColor(Color.parseColor(remoteConfig.getString("theme_power_text_color")))
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

        val settingsButton: Button = simpleEventButton(
                getString(R.string.settings), {
            val settingIntent = Intent(this, SettingsActivity::class.java).run {
                putExtra(PRISONER_POWER, prisoner.power)
            }
            settingIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivityIfNeeded(settingIntent, 0)
        })

        /* Temporary Buttons */
        val tempAdd1000PowerButton: Button = simpleEventButton(
                "+1000 Power", {prisonerEvents.tempAdd1000Power()})

        /*
        val tempCapturedButton: Button = simpleEventButton(
                "Captured", {prisonerEvents.eventCaptured()})

        val tempDiedButton: Button = simpleEventButton(
                "Died", {prisonerEvents.died()})

        val tempPraiseTheSunButton: Button = simpleEventButton(
                "Praise Sun", {prisonerEvents.eventPraiseTheSun()})

        val tempGodKillButton: Button = simpleEventButton(
                "God Kill", {prisonerEvents.eventGodKill()})

        val tempWonButton: Button = simpleEventButton(
                "Win", {prisonerEvents.eventWon()})

        val tempEscapeNotificationButton: Button = simpleEventButton(
                "Notify", {escapeNotification()})
                */

        return arrayOf<Button>(
                tempAdd1000PowerButton,
                pushUpButton,
                sitUpButton,
                tryEscapeButton,
                settingsButton
                        /*
                tempCapturedButton,
                tempDiedButton,
                tempPraiseTheSunButton,

                tempGodKillButton,

                tempWonButton,
                tempEscapeNotificationButton,*/
                )
    }

    /* Geofence-related */
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
}
