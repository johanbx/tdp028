package nu.jobo.prison

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

class AnalyticEvents(private val mFirebaseAnalytics: FirebaseAnalytics) {

    fun shareApp(uid: String, power: Int) {
        val params = Bundle()
        params.putString("user_uid", uid)
        params.putInt("power", power)
        mFirebaseAnalytics.logEvent("share_app", params)
    }

    fun wasInvited(uid: String) {
        val params = Bundle()
        params.putString("user_uid", uid)
        mFirebaseAnalytics.logEvent("was_invited", params)
    }

    fun soundSwitch(uid: String, isOn: Boolean) {
        val params = Bundle()
        params.putString("user_uid", uid)
        params.putBoolean("sound_on", isOn)
        mFirebaseAnalytics.logEvent("sound_switch", params)
    }

    fun gameWon(uid: String, power: Int) {
        val params = Bundle()
        params.putString("user_uid", uid)
        params.putInt("power", power)
        mFirebaseAnalytics.logEvent("game_won", params)
    }

    fun changedLanguage(uid: String, language: String) {
        val params = Bundle()
        params.putString("user_uid", uid)
        params.putString("language", language)
        mFirebaseAnalytics.logEvent("change_language", params)
    }
}