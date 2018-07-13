package nu.jobo.prison

import android.os.Bundle

class AnalyticEvents(private val mainActivity: MainActivity) {
    fun shareApp(uid: Int, power: Int) {
        val params = Bundle()
        params.putInt("user_uid", uid)
        params.putInt("power", power)
        mainActivity.mFirebaseAnalytics.logEvent("share_app", params)
    }

    fun wasInvited(uid: Int) {
        val params = Bundle()
        params.putInt("user_uid", uid)
        mainActivity.mFirebaseAnalytics.logEvent("was_invited", params)
    }

    fun soundSwitch(uid: Int, isOn: Boolean) {
        val params = Bundle()
        params.putInt("user_uid", uid)
        params.putBoolean("sound_on", isOn)
        mainActivity.mFirebaseAnalytics.logEvent("sound_switch", params)
    }

    fun musicSwitch(uid: Int, isOn: Boolean) {
        val params = Bundle()
        params.putInt("user_uid", uid)
        params.putBoolean("music_on", isOn)
        mainActivity.mFirebaseAnalytics.logEvent("music_switch", params)
    }

    fun changedLanguage(uid: Int, language: String) {
        val params = Bundle()
        params.putInt("user_uid", uid)
        params.putString("language", language)
        mainActivity.mFirebaseAnalytics.logEvent("change_language", params)
    }
}