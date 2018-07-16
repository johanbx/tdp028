package nu.jobo.prison

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_settings.*



class SettingsActivity : Activity() {

    companion object {
        const val TAG = "MY_SETTINGS"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

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
    }
}
