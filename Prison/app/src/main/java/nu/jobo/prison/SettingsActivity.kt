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

        syncCloudSaveButton.setOnClickListener {
            val loginOnMainActivity = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(MainActivity.INTENT_LOGIN, MainActivity.INTENT_LOGIN)
            }
            startActivityIfNeeded(loginOnMainActivity, 0)
        }

        deleteAccountButton.setOnClickListener {
            val deleteCurrentUser = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(MainActivity.INTENT_DELETE_ACCOUNT, MainActivity.INTENT_DELETE_ACCOUNT)
            }
            startActivityIfNeeded(deleteCurrentUser, 0)
        }

        logoutButton.setOnClickListener {
            val logoutCurrentUser = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(MainActivity.INTENT_LOGOUT, MainActivity.INTENT_LOGOUT)
            }
            startActivityIfNeeded(logoutCurrentUser, 0)
        }

        soundSwitch.setOnCheckedChangeListener { compoundButton, b ->
            MainActivity.mediaPlayerMuted = !b
            when (b) {
                false -> MainActivity.mediaPlayer.setVolume(0.0f,0.0f)
                true -> MainActivity.mediaPlayer.setVolume(1.0f,1.0f)
            }
        }

        shareGameButton.setOnClickListener {
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

        languageEnglishButton.setOnClickListener{
            analyticEvents.changedLanguage(mAuth.currentUser!!.uid, "English")
            LocaleManager.setNewLocale(this, LocaleManager.LANGUAGE_ENGLISH)
            refreshActivity()
        }

        languageSwedishButton.setOnClickListener{
            analyticEvents.changedLanguage(mAuth.currentUser!!.uid, "Swedish")
            LocaleManager.setNewLocale(this, LocaleManager.LANGUAGE_SWEDISH)
            refreshActivity()
        }
    }

    private fun refreshActivity() {
        val refreshIntent = Intent(intent).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(LANGUAGE_CHANGED, true)
        }
        startActivity(refreshIntent)
    }

    override fun onBackPressed() {
        if (intent.getBooleanExtra(LANGUAGE_CHANGED, false)) {
            intent.removeExtra(LANGUAGE_CHANGED)
            startActivity(Intent(this, MainActivity::class.java))
            return
        } else {
            super.onBackPressed()
        }
    }
}
