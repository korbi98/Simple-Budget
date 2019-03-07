/*
 * Copyright 2019 Korbinian Moser
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, MainPreferenceFragment())
                .commit()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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
