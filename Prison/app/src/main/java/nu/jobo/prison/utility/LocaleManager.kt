package nu.jobo.prison.utility

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.preference.PreferenceManager
import java.util.*


// Source: https://proandroiddev.com/change-language-programmatically-at-runtime-on-android-5e6bc15c758
// https://github.com/YarikSOffice/LanguageTest/blob/master/app/src/main/java/com/yariksoffice/languagetest/LocaleManager.java
public class LocaleManager {
    companion object {

        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_SWEDISH = "sv"
        private const val LANGUAGE_KEY = "LANGUAGE_KEY"

        fun setLocale(c: Context): Context {
            return setNewLocale(c, getLanguage(c));
        }

        fun setNewLocale(c: Context, language: String): Context {
            persistLanguage(c, language);
            return updateResources(c, language);
        }

        private fun getLanguage(c: Context): String {
            val prefs = PreferenceManager.getDefaultSharedPreferences(c)
            return prefs.getString(LANGUAGE_KEY, LANGUAGE_ENGLISH)
        }

        private fun persistLanguage(c: Context, language: String) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(c)
            prefs.edit().putString(LANGUAGE_KEY, language).commit()
        }

        private fun updateResources(context: Context, language: String): Context {
            val locale = Locale(language)
            Locale.setDefault(locale)

            val res = context.resources
            val config = Configuration(res.configuration)
            if (Build.VERSION.SDK_INT >= 17) {
                config.setLocale(locale)
                return context.createConfigurationContext(config)
            } else {
                config.locale = locale
                res.updateConfiguration(config, res.displayMetrics)
                return context
            }
        }
    }
}