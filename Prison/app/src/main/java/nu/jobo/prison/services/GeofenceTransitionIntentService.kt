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

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.setLocale(this)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(newBase!!))
    }

    override fun onHandleIntent(intent: Intent?) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Error Code: " + geofencingEvent.errorCode.toString())
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                sendNotification(getString(R.string.fence_notification_inside_title),
                        getString(R.string.fence_notification_inside_message))
            } else {
                if (MainActivity.escaped) {
                    sendNotification(getString(R.string.won_the_game_not_title), getString(R.string.click_for_highscore), true)
                } else {
                    sendNotification("You were captured by the guards",
                            "You were not fast enough")
                }
            }

            // Get the transition details as a String and log it.
            val geofenceTransitionDetails = getGeofenceTransitionDetails(
                    geofenceTransition,
                    triggeringGeofences
            )
            Log.i(TAG, geofenceTransitionDetails)
        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type))
        }
    }


    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
    private fun sendNotification(title: String, message: String, wonGame: Boolean = false) {
        // Get an instance of the Notification manager
        val mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            // Create the channel for the notification
            val mChannel = NotificationChannel(MainActivity.CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel)
        }

        var notificationPendingIntent: PendingIntent? = null
        if (wonGame) {
            val notificationIntent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(MainActivity.INTENT_WON_GAME, MainActivity.INTENT_WON_GAME)
            }

            // Construct a task stack.
            val stackBuilder = TaskStackBuilder.create(this)

            // Add the main Activity to the task stack as the parent.
            stackBuilder.addParentStack(MainActivity::class.java)

            // Push the content Intent onto the stack.
            stackBuilder.addNextIntent(notificationIntent)

            // Get a PendingIntent containing the entire back stack.
            notificationPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        // Get a notification builder that's compatible with platform versions >= 4
        val builder = NotificationCompat.Builder(this)

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setContentTitle(title)
                .setContentText(message)

        if (wonGame) {
            builder.setContentIntent(notificationPendingIntent)
        }

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(MainActivity.CHANNEL_ID) // Channel ID
        }

        // Dismiss notification once the user touches it.
        // builder.setAutoCancel(true)

        // Issue the notification
        mNotificationManager.notify(0, builder.build())
    }

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