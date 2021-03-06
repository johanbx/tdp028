package nu.jobo.prison.activities

import nu.jobo.prison.events.AnalyticEvents
import nu.jobo.prison.utility.LocaleManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_settings.*
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.database.FirebaseDatabase
import nu.jobo.prison.R


class SettingsActivity : Activity() {

    companion object {
        const val TAG = "MY_SETTINGS"
        const val LANGUAGE_CHANGED = "LANGUAGE_CHANGED"
    }

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics
    private lateinit var mAuth: FirebaseAuth
    private lateinit var analyticEvents: AnalyticEvents

    // Applies language settings without a restart
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        LocaleManager.setLocale(this)
    }

    // Gives localmanager this context (used in languages)
    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LocaleManager.setLocale(newBase!!))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Firebase
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this)
        mAuth = FirebaseAuth.getInstance()
        analyticEvents = AnalyticEvents(mFirebaseAnalytics)

        // Sound option
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        soundSwitch.isChecked = !prefs.getBoolean(MainActivity.VOLUME_MUTED, false)

        // Bind functions to buttons
        syncCloudSaveButton.setOnClickListener { syncCloudSave() }
        deleteAccountButton.setOnClickListener { deleteUser() }
        logoutButton.setOnClickListener { logoutUser() }
        shareGameButton.setOnClickListener { shareGame() }

        // On switch change, set volue on/off accordingly
        soundSwitch.setOnCheckedChangeListener { _, b ->
            val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            prefs.edit().putBoolean(MainActivity.VOLUME_MUTED, !b).commit()
            when (b) {
                false -> MainActivity.mediaPlayer.setVolume(0.0f,0.0f)
                true -> {
                    MainActivity.mediaPlayer.setVolume(1.0f,1.0f)
                }
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

    // Opens text intent that writes a message with current prisoner power and dynamic link
    private fun shareGame() {
        val power = intent.getIntExtra(MainActivity.PRISONER_POWER, 0)
        analyticEvents.shareApp(mAuth.currentUser!!.uid, power)

        val shortLink = "https://prison.page.link/invite"
        val msg =  "I found this awesome game called Prison! Can you " +
                "escape before I can? Can you beat my $power power? $shortLink"
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.putExtra(Intent.EXTRA_TEXT, msg)
        shareIntent.type = "text/plain"
        startActivity(shareIntent)
    }

    // Send login intent to mainactivity
    private fun syncCloudSave(force: Boolean = false) {
        val loginOnMainActivity = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(MainActivity.INTENT_LOGIN, MainActivity.INTENT_LOGIN)
            if (force) {
                putExtra(MainActivity.INTENT_LOGIN_FORCE, MainActivity.INTENT_LOGIN_FORCE)
            }
        }
        startActivity(loginOnMainActivity)
        finish()
    }

    // OBS: This is not a restart. Used when language is changed.
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

    // Logout current user
    private fun logoutUser() {
        when (mAuth.currentUser){
            null -> {
                Log.e(TAG, getString(R.string.error_logout_null_user))
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

    // Delete current user
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
                    Toast.makeText(this, getString(R.string.removed_user), Toast.LENGTH_SHORT).show()
                    restartApp()
                } else {
                    if (it.exception is FirebaseAuthRecentLoginRequiredException) {
                        // prompt login if user cant delete itself
                        Toast.makeText(this, getString(R.string.login_to_delete_account), Toast.LENGTH_SHORT).show()
                        syncCloudSave(true)
                    } else {
                        Toast.makeText(this, getString(R.string.unknown_error), Toast.LENGTH_SHORT).show()
                        Log.e(TAG, it.exception.toString())
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.failed_to_delete_user), Toast.LENGTH_SHORT).show()
            Log.e(TAG, e.toString())
        }
    }

    // When backbutton is pressed go directly to mainactivity (if e.g. a refresh activity was issued)
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
