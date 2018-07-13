package nu.jobo.prison

import android.os.Bundle

class AnalyticEvents(private val mainActivity: MainActivity) {
    fun shareApp(uid: String, power: Int) {
        val params = Bundle()
        params.putString("user_uid", uid)
        params.putInt("power", power)
        mainActivity.mFirebaseAnalytics.logEvent("share_app", params)
    }

    fun wasInvited(uid: String) {
        val params = Bundle()
        params.putString("user_uid", uid)
        mainActivity.mFirebaseAnalytics.logEvent("was_invited", params)
    }

    fun soundSwitch(uid: String, isOn: Boolean) {
        val params = Bundle()
        params.putString("user_uid", uid)
        params.putBoolean("sound_on", isOn)
        mainActivity.mFirebaseAnalytics.logEvent("sound_switch", params)
    }

    fun musicSwitch(uid: String, isOn: Boolean) {
        val params = Bundle()
        params.putString("user_uid", uid)
        params.putBoolean("music_on", isOn)
        mainActivity.mFirebaseAnalytics.logEvent("music_switch", params)
    }

    fun changedLanguage(uid: String, language: String) {
        val params = Bundle()
        params.putString("user_uid", uid)
        params.putString("language", language)
        mainActivity.mFirebaseAnalytics.logEvent("change_language", params)
    }
}