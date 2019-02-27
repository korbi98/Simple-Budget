package com.korbi.simplebudget.ui

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import android.preference.RingtonePreference
import android.text.TextUtils
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.korbi.simplebudget.BuildConfig
import com.korbi.simplebudget.R



class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupActionBar()

        supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, MainPreferenceFragment())
                .commit()
    }


    private fun setupActionBar() {
        actionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class MainPreferenceFragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootkey: String?) {

            setPreferencesFromResource(R.xml.preferences, rootkey)

            val packageName = BuildConfig.APPLICATION_ID
            val versionNumber = BuildConfig.VERSION_CODE

            val version = findPreference<Preference>(getString(R.string.about_version_number_key))
            version.summary = versionNumber.toString()

            val sendFeedback = findPreference<Preference>(getString(R.string.key_send_feedback))
            sendFeedback.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                val mailLink = "mailto:info@korbinian-moser.de?" + "subject=Simple Budget feedback"

                val sendMail = Intent(Intent.ACTION_VIEW)
                val data = Uri.parse(mailLink)
                sendMail.data = data
                startActivity(sendMail)
                true
            }

            val rateApp = findPreference<Preference>(getString(R.string.about_rate_app_key))
            rateApp.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=$packageName")))
                } catch (e: android.content.ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=$packageName")))
                }

                true
            }

            val otherApps = findPreference<Preference>(getString(R.string.about_other_apps_key))
            otherApps.onPreferenceClickListener = Preference.OnPreferenceClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://developer?id=Korbinian+Moser")))
                } catch (e: android.content.ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/developer?id=Korbinian+Moser")))
                }

                true
            }
        }
    }

}
