package nu.jobo.prison.services

import nu.jobo.prison.utility.LocaleManager
import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import android.text.TextUtils
import android.os.Build
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Context
import android.content.res.Configuration
import android.support.v4.app.NotificationCompat
import android.support.v4.app.TaskStackBuilder
import nu.jobo.prison.activities.MainActivity
import nu.jobo.prison.R


// Some snippets taken from:
// https://github.com/googlesamples/android-play-location/blob/master/Geofencing/app/src/main/java/com/google/android/gms/location/sample/geofencing/GeofenceTransitionsJobIntentService.java

// Others taken from:
// https://developer.android.com/training/location/geofencing#kotlin

class GeofenceTransitionsIntentService : IntentService("GeofenceService") {

    companion object {
        const val TAG = "MY_GEOFENCE"
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

    // Triggers on geofence events (works in the background)
    override fun onHandleIntent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Error Code: " + geofencingEvent.errorCode.toString())
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                sendNotification(getString(R.string.fence_notification_inside_title), getString(R.string.fence_notification_inside_message))
            } else {
                if (MainActivity.escaped) {
                    sendNotification(getString(R.string.won_the_game_not_title), getString(R.string.click_for_highscore), true)
                } else {
                    // Todo: Set "escaped" to false after some time. Right now this is never called.
                    sendNotification("You were captured by the guards",
                            "You were not fast enough")
                }
            }

            // Since we only have one fence we do not need to worry about which
            // fence that was activated. This is mainly for logging purposes.
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val geofenceTransitionDetails = getGeofenceTransitionDetails(
                    geofenceTransition,
                    triggeringGeofences)
            Log.i(TAG, geofenceTransitionDetails)
        } else {
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type))
        }
    }

    // Notification which opens up mainactivity if the game was won
    private fun sendNotification(title: String, message: String, wonGame: Boolean = false) {
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val mChannel = NotificationChannel(MainActivity.CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
            mNotificationManager.createNotificationChannel(mChannel)
        }

        var notificationPendingIntent: PendingIntent? = null
        if (wonGame) {
            // Send out intent that opens mainactivity if the game was won
            val notificationIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(MainActivity.INTENT_WON_GAME, MainActivity.INTENT_WON_GAME)
            }

            val stackBuilder = TaskStackBuilder.create(this)
            stackBuilder.addParentStack(MainActivity::class.java)
            stackBuilder.addNextIntent(notificationIntent)
            notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val builder = NotificationCompat.Builder(this)
        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)

        if (wonGame) {
            builder.setContentIntent(notificationPendingIntent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(MainActivity.CHANNEL_ID)
        }

        mNotificationManager.notify(0, builder.build())
    }

    // Used for logging
    private fun getGeofenceTransitionDetails(
            geofenceTransition: Int,
            triggeringGeofences: List<Geofence>): String {

        val geofenceTransitionString = getTransitionString(geofenceTransition)

        // Get the Ids of each geofence that was triggered.
        val triggeringGeofencesIdsList = mutableListOf<String>()
        for (geofence in triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.requestId)
        }
        val triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList)

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString
    }

    private fun getTransitionString(transitionType: Int): String {
        return when (transitionType) {
            Geofence.GEOFENCE_TRANSITION_ENTER ->
                getString(R.string.geofence_transition_entered)

            Geofence.GEOFENCE_TRANSITION_EXIT ->
                getString(R.string.geofence_transition_exited)

            else -> getString(R.string.unknown_geofence_transition)
        }
    }
}