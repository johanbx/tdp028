package nu.jobo.prison.activities

import nu.jobo.prison.events.AnalyticEvents
import nu.jobo.prison.events.PrisonerEvents
import nu.jobo.prison.utility.LocaleManager
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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.google.android.gms.location.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.android.synthetic.main.activity_main.*
import nu.jobo.prison.BuildConfig
import nu.jobo.prison.R
import nu.jobo.prison.datacontainers.PrisonerData
import nu.jobo.prison.services.GeofenceTransitionsIntentService


class MainActivity : Activity(), SensorEventListener {

    companion object {
        const val TAG = "MY_MAINACTIVITY"

        const val SIGN_IN_AND_ASK = 120
        const val SIGN_IN_AND_EXIT = 121
        const val CHANNEL_ID = "CHANNEL_ID"
        const val FIRST_TIME_RUN = "FIRST_TIME_RUN"

        const val FENCE_KEY = "FENCE_KEY"
        const val FENCE_RADIUS_METER: Float = 100f
        const val FENCE_EXPIRATION_MILLISECONDS: Long = 200000

        const val LOCATION_PERMISSION_REQUEST_CODE = 345

        const val INTENT_LOGIN = "INTENT_LOGIN"
        const val INTENT_LOGIN_FORCE = "INTENT_LOGIN_FORCE"
        const val INTENT_WON_GAME = "INTENT_WON_GAME"

        const val PRISONER_POWER = "PRISONER_POWER"

        const val MUSIC_POSITION = "MUSIC_POSITION"
        const val VOLUME_MUTED = "VOLUME_MUTED"

        // Todo: Perhaps move these somewhere else
        lateinit var mediaPlayer: MediaPlayer // Used in SettingsActivity
        var escaped = false // Used in PrisonerEvents & GeofenceService
    }

    // Misc
    private var firstTimeRun = false

    // Steps
    private var running = false
    private var sensorManager:SensorManager? = null

    // Game Elements
    var prisoner = PrisonerData()
    var startingPower = 0
    private lateinit var escapingAttemptNotificationBuilder: NotificationCompat.Builder
    private lateinit var prisonerEvents: PrisonerEvents

    // Geofence & Location
    private lateinit var fence: Geofence
    private lateinit var geofencingClient: GeofencingClient
    private var fenceLatitude: Double = 0.0
    private var fenceLongitude: Double = 0.0

    // Firebase
    // Authentication Providers (login ui)
    private var providers = Arrays.asList(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.FacebookBuilder().build())
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var analyticEvents: AnalyticEvents
    private lateinit var remoteConfig: FirebaseRemoteConfig

    // UI
    lateinit var stepCounter: TextView
    lateinit var statusImage: ImageView
    lateinit var pushUpCounter: TextView
    lateinit var sitUpCounter: TextView
    lateinit var powerCounter: TextView
    lateinit var oldPrisoner: PrisonerData

    // "On" events
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        setContentView(R.layout.activity_main)
        setActionBar(findViewById(R.id.actionBar))

        initRemoteConfig()
        applyTheme()
        createNotificationChannel()
        initNotification()

        mAuth = FirebaseAuth.getInstance()
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        prisonerEvents = PrisonerEvents(this)
        analyticEvents = AnalyticEvents(mFirebaseAnalytics)

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

        pushUpButton.setOnClickListener { prisonerEvents.pushUp() }
        sitUpButton.setOnClickListener { prisonerEvents.sitUp() }
        escapeButton.setOnClickListener { prisonerEvents.tryEscape() }

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
                        startingPower = 2000 }}

        loginUserOnStart()

        setTitle(R.string.prisoner_status_captured)
        initMediaPlayer(savedInstanceState?.getInt(MUSIC_POSITION, 0)?: 0)
    }

    // Used in Toolbar Menu
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_settings -> {
                val settingIntent = Intent(this, SettingsActivity::class.java).run {
                    putExtra(PRISONER_POWER, prisoner.power)
                }
                settingIntent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivityIfNeeded(settingIntent, 0)
            }
        }
        return true
    }

    // Initializes Toolbar Menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.actionbar, menu)
        return true
    }

    // Saves music position
    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.run {
            putInt(MUSIC_POSITION, mediaPlayer.currentPosition)
        }
        super.onSaveInstanceState(outState)
    }

    // Applies language settings without a restart
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.setLocale(this)
    }

    // Gives localmanager this context (used in languages)
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(newBase!!))
    }

    // Handles Intent, signs in user
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

    // Handles Location Permissions
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

    // With this we can use Intent parameters in "onresume" (bypasses oncreate)
    override fun onNewIntent(intent: Intent?) {
        if (intent != null) {
            setIntent(intent)
        }
    }

    // Starts mediaplayer. Check/register step sensor. Handles sign in intents and won intent.
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")

        // Media player
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }

        // Steps
        // Source for step counter:
        // https://medium.com/@ssaurel/create-a-step-counter-fitness-app-for-android-with-kotlin-bbfb6ffe3ea7
        running = true
        val stepsSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        if (stepsSensor == null) {
            Toast.makeText(this, "No Step Counter Sensor !", Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepsSensor, SensorManager.SENSOR_DELAY_UI)
        }

        // Intents
        if (intent.getStringExtra(INTENT_LOGIN) == INTENT_LOGIN) {
            intent.removeExtra(INTENT_LOGIN)
            if (intent.getStringExtra(INTENT_LOGIN_FORCE) == INTENT_LOGIN_FORCE) {
                intent.removeExtra(INTENT_LOGIN_FORCE)
                loginUser(SIGN_IN_AND_EXIT, true)
            } else {
                loginUser(SIGN_IN_AND_ASK)
            }
        } else if (intent.getStringExtra(INTENT_WON_GAME) == INTENT_WON_GAME) {
            Log.d(TAG, "User won the game")
            intent.removeExtra(INTENT_WON_GAME)
            prisonerEvents.eventWon()
            analyticEvents.gameWon(mAuth.currentUser!!.uid, prisoner.power)
        }
    }

    // Pauses sensormanager
    override fun onPause() {
        super.onPause()
        running = false
        sensorManager?.unregisterListener(this)
    }

    // Pauses mediaplayer
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop called")
        mediaPlayer.pause()
    }

    // Destroys mediaplayer (otherwise it will keep playing in background)
    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer.release()
        } catch (e: UninitializedPropertyAccessException) {
            Log.e(TAG, "Mediaplayer could not be released. " +
                    "This always occurs on first startup")
        }
    }

    // Required for steps
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // On each step call a step prisonerevent
    override fun onSensorChanged(event: SensorEvent?) {
        if (running && event != null) {
            prisonerEvents.step()
        }
    }

    // Init:s
    private fun initMediaPlayer(musicPosition: Int = 0) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        mediaPlayer = MediaPlayer.create(applicationContext, R.raw.background_music)
        mediaPlayer.seekTo(musicPosition)
        mediaPlayer.isLooping = true
        if (prefs.getBoolean(VOLUME_MUTED, false)) {
            mediaPlayer.setVolume(0.0f, 0.0f)
        }
        mediaPlayer.start()
    }

    // Listens to database changes and updates prisoner and ui accordingly
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

    // initializes escape notification
    private fun initNotification(){
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        intent.action = Intent.ACTION_MAIN

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        escapingAttemptNotificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(getString(R.string.successful_escape))
                .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.on_escape_instructions)))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
    }

    // Creates geofence on current position
    @SuppressLint("MissingPermission")
    fun initGeofence() {
        fence = Geofence.Builder()
                .setRequestId(FENCE_KEY)
                .setCircularRegion(
                        fenceLatitude,
                        fenceLongitude,
                        FENCE_RADIUS_METER)
                .setExpirationDuration(FENCE_EXPIRATION_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()

        geofencingClient.addGeofences(getGeofencingRequest(), geofencePendingIntent)?.run {
            addOnSuccessListener {
                Toast.makeText(applicationContext, "success: Geofences added", Toast.LENGTH_SHORT).show()
            }
            addOnFailureListener {
                Toast.makeText(applicationContext, "failure: Geofences not added", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fetches remote config and applies it on success
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

    // Alertdialog which asks if prisoner should use old
    // prisonerdata or new prisonerdata
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

    // Login a user on start. Either anonymous or with FA
    private fun loginUserOnStart() {
        if (mAuth.currentUser == null){
            createAndLoginAnonymousUser()
        } else {
            initOnDatabaseChanges()
        }
    }

    // OBS: onActivityResult for actual logic
    // TODO: See Project in Github
    // Login FA user with firebase ui activity
    private fun loginUser(signInIntentCode: Int, force: Boolean = false) {
        if (mAuth.currentUser!!.isAnonymous || force) {
            val intent = AuthUI.getInstance()
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

    // Sign in anonymously (creates anonymous user)
    private fun createAndLoginAnonymousUser() {
        mAuth.signInAnonymously()
                .addOnCompleteListener {
                    initOnDatabaseChanges()
                    startWelcomeDialog(startingPower > 0)
                }
                .addOnFailureListener {
                    Toast.makeText(this, getString(R.string.unknown_login_error), Toast.LENGTH_SHORT).show()
                    finish() }
    }

    // Misc
    private fun restartApp() {
        finish()
        val restartActivity = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        mediaPlayer.release()
        startActivity(restartActivity)
    }

    // Welcome dialog that explains game purpose and if player got power bonus from friend invite
    private fun startWelcomeDialog(wasInvited: Boolean = false) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        firstTimeRun = prefs.getBoolean(FIRST_TIME_RUN, true)
        if (firstTimeRun) {
            prefs.edit().putBoolean(FIRST_TIME_RUN, false).apply()
            var inviteMsg = ""
            when (wasInvited) {
                true -> {
                    inviteMsg = getString(R.string.friend_invite_extra_power)
                    prisoner.power = startingPower
                    analyticEvents.wasInvited(mAuth.currentUser?.uid!!)
                    dbUpdate {
                        updateUI()
                    }
                }
                false -> inviteMsg = ""
            }
            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder
                    .setMessage(
                            getString(R.string.welcome_to_prison) + "\n" +
                                    getString(R.string.goal_of_game) + "\n" +
                                    getString(R.string.on_escape_instructions) + "\n" +
                                    inviteMsg)
                    .create()
                    .show()
        }
    }

    // Updates the cloud database with current prisoner values
    fun dbUpdate(function: () -> Unit) {
        if (mAuth.currentUser != null) {
            val ref = FirebaseDatabase
                    .getInstance()
                    .getReference("users/" + mAuth.currentUser!!.uid)
            ref.setValue(prisoner)
            function()
        }
    }

    // Help function for permission requirements
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

    // Create notification channel (required for Android O)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    // Fires of escape notification
    fun escapeNotification() {
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(123, escapingAttemptNotificationBuilder.build())
    }

    // UI-related
    // Updates UI with counter values, power, name and progressbar
    private fun updateUI() {
        if (mAuth.currentUser?.displayName.isNullOrEmpty()) {
            usernameTextView.text = getString(R.string.anonymous)
        } else {
            usernameTextView.text = mAuth.currentUser?.displayName
        }
        escapeProgressBar.progress = prisoner.power
        pushUpCounter.text = prisoner.pushUps.toString()
        sitUpCounter.text = prisoner.sitUps.toString()
        powerCounter.text =  prisoner.power.toString()
        stepCounter.text = prisoner.steps.toString()
    }

    // Used in Remote Configuration,
    // sets some elements text color with a dynamic value
    private fun applyTheme() {
        powerTextView.setTextColor(Color.parseColor(remoteConfig.getString("power_text_view_color")))
        stepsTextView.setTextColor(Color.parseColor(remoteConfig.getString("steps_text_view_color")))
        pushUpsTextView.setTextColor(Color.parseColor(remoteConfig.getString("push_ups_text_view_color")))
        sitUpsTextView.setTextColor(Color.parseColor(remoteConfig.getString("sit_ups_text_view_color")))
    }

    // Geofence-related
    // Builds the geofence with initial trigger on enter, adds current fence(s)
    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(listOf<Geofence>(fence))
        }.build()
    }

    // The event for the geofence which starts the GeofenceTransitionIntentService
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
        PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
