package nu.jobo.prison

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        syncCloudSaveButton.setOnClickListener {
            val loginOnMainActivity = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(MainActivity.INTENT_LOGIN, MainActivity.INTENT_LOGIN)
            }
            startActivityIfNeeded(loginOnMainActivity, 0)
            finish()
        }

        deleteAccountButton.setOnClickListener {
            val deleteCurrentUser = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(MainActivity.INTENT_DELETE_ACCOUNT, MainActivity.INTENT_DELETE_ACCOUNT)
            }
            startActivityIfNeeded(deleteCurrentUser, 0)
            finish()
        }

        logoutButton.setOnClickListener {
            val logoutCurrentUser = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(MainActivity.INTENT_LOGOUT, MainActivity.INTENT_LOGOUT)
            }
            startActivityIfNeeded(logoutCurrentUser, 0)
            finish()
        }
    }
}
