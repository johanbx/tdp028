package nu.jobo.prison

import android.app.Activity
import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import kotlinx.android.synthetic.main.activity_firebase_link.*

class WelcomeActivity : Activity() {

    var GIVE_FREE_POWER = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_firebase_link)

        startGameButton.setOnClickListener { startGameActivity() }

        FirebaseDynamicLinks.getInstance().getDynamicLink(intent)
            .addOnFailureListener{
                Log.e(MainActivity.TAG, it.toString())}
            .addOnSuccessListener {
                if (it != null) {
                    Toast.makeText(applicationContext,
                            "Firebase Link Detected", Toast.LENGTH_SHORT).show()
                    friendInviteTextView.visibility = View.VISIBLE
                    GIVE_FREE_POWER = true
                }
            }
    }

    fun startGameActivity() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (GIVE_FREE_POWER) {
            intent.putExtra(MainActivity.FRIEND_INVITE_POWER_BONUS, 2000)
        }

        startActivity(intent)
    }
}
