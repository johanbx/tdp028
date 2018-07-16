package nu.jobo.prison

import android.app.Activity
import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_settings.*
import android.os.LocaleList
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.database.FirebaseDatabase


class SettingsActivity : Activity() {

    companion object {
        const val TAG = "MY_SETTINGS"
        const val LANGUAGE_CHANGED = "LANGUAGE_CHANGED"
    }

    lateinit var mFirebaseAnalytics: FirebaseAnalytics
    lateinit var mAuth: FirebaseAuth
    lateinit var analyticEvents: AnalyticEvents

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.setLocale(this)
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(newBase!!))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mAuth = FirebaseAuth.getInstance()

        analyticEvents = AnalyticEvents(mFirebaseAnalytics)

        soundSwitch.isChecked = !MainActivity.mediaPlayerMuted

        syncCloudSaveButton.setOnClickListener { syncCloudSave() }
        deleteAccountButton.setOnClickListener { deleteUser() }
        logoutButton.setOnClickListener { logoutUser() }
        shareGameButton.setOnClickListener { shareGame() }

        soundSwitch.setOnCheckedChangeListener { _, b ->
            MainActivity.mediaPlayerMuted = !b
            when (b) {
                false -> MainActivity.mediaPlayer.setVolume(0.0f,0.0f)
                true -> MainActivity.mediaPlayer.setVolume(1.0f,1.0f)
            }
        }

        languageEnglishButton.setOnClickListener{
            analyticEvents.changedLanguage(mAuth.currentUser!!.uid, "English")
            LocaleManager.setNewLocale(this, LocaleManager.LANGUAGE_ENGLISH)
            refreshActivity(true)
        }

        languageSwedishButton.setOnClickListener{
            analyticEvents.changedLanguage(mAuth.currentUser!!.uid, "Swedish")
            LocaleManager.setNewLocale(this, LocaleManager.LANGUAGE_SWEDISH)
            refreshActivity(true)
        }
    }

    private fun shareGame() {
        val power = intent.getIntExtra(MainActivity.PRISONER_POWER, 0)
        analyticEvents.shareApp(mAuth.currentUser!!.uid, power)

        val shortLink = "https://prison.page.link/invite"
        val msg =  "I found this awesome game called Prison! Can you " +
                "escape before I can? Can you beat my $power power? $shortLink"
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, msg)
        sendIntent.type = "text/plain"
        refreshActivity()
    }

    private fun syncCloudSave() {
        val loginOnMainActivity = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(MainActivity.INTENT_LOGIN, MainActivity.INTENT_LOGIN)
        }
        startActivity(loginOnMainActivity)
        finish()
    }

    private fun refreshActivity(languageChanged: Boolean = false) {
        val refreshIntent = Intent(intent).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (languageChanged) {
                putExtra(LANGUAGE_CHANGED, true)
            }
        }
        startActivity(refreshIntent)
    }

    private fun restartApp() {
        finish()
        val restartActivity = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(restartActivity)
    }

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

    private fun deleteUser() {
        try {
            // Database
            FirebaseDatabase
                    .getInstance()
                    .getReference("users/" + mAuth.currentUser!!.uid)
                    .removeValue()

            // User
            mAuth.currentUser!!.delete().addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Removed user", Toast.LENGTH_SHORT).show()
                    restartApp()
                } else {
                    if (it.exception is FirebaseAuthRecentLoginRequiredException?) {
                        // prompt login if user cant delete itself
                        Toast.makeText(this, getString(R.string.login_to_delete_account), Toast.LENGTH_SHORT).show()
                        syncCloudSave()
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

    override fun onBackPressed() {
        if (intent.getBooleanExtra(LANGUAGE_CHANGED, false)) {
            intent.removeExtra(LANGUAGE_CHANGED)
            Log.d(TAG, "On back pressed was overridden")
            startActivity(Intent(this, MainActivity::class.java))
            return
        } else {
            super.onBackPressed()
        }
    }
}
