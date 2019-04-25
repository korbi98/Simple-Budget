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

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.korbi.simplebudget.BuildConfig
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.ui.dialogs.CurrencyDialog


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

    class MainPreferenceFragment : PreferenceFragmentCompat(), CurrencyDialog.OnDismissListener {

        private var currency: Preference? = null
        private lateinit var currencyList: Array<String>
        private lateinit var currencySymbols: Array<String>
        private lateinit var pref: SharedPreferences

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootkey: String?) {

            setPreferencesFromResource(R.xml.preferences, rootkey)
            pref = PreferenceManager.getDefaultSharedPreferences(context)

            val packageName = BuildConfig.APPLICATION_ID
            val versionNumber = BuildConfig.VERSION_NAME

            currencyList = resources.getStringArray(R.array.currencies_with_names)
            currencySymbols = resources.getStringArray(R.array.currencies_symbols)

            val symbol = pref.getString(
                    getString(R.string.settings_key_currency), currencySymbols[0])

            currency = findPreference(getString(R.string.settings_key_currency))
            currency?.let {
                it.summary = when {
                    currencySymbols.contains(symbol) -> {
                        currencyList[currencySymbols.indexOf(symbol)]
                    }
                    else -> symbol
                }
                it.setOnPreferenceClickListener {
                    CurrencyDialog().run {
                        setTargetFragment(this@MainPreferenceFragment, 0)
                        setListener(this@MainPreferenceFragment)
                        show(this@MainPreferenceFragment.requireFragmentManager(), "currency_dialog")
                    }
                    true
                }
            }

            val historyGrouping = findPreference<ListPreference>(
                    getString(R.string.settings_key_history_grouping))
            historyGrouping?.setSummaryProvider {
                updateWidget()
                (it as ListPreference).entry
            }

            //TODO implement import/export
            val exportExpenses = findPreference<Preference>(getString(R.string.export_expenses_key))

            val importExpenses = findPreference<Preference>(getString(R.string.import_expenses_key))

            val resetDatabase = findPreference<Preference>(getString(R.string.reset_db_key))
            resetDatabase?.setOnPreferenceClickListener {
                AlertDialog.Builder(requireContext()).apply {
                    setTitle(R.string.dialog_db_reset_title)
                    setMessage(R.string.dialog_db_reset_message)
                    setPositiveButton(R.string.ok) { dialog, _ ->
                        DBhandler.getInstance().resetDatabase()
                        dialog.dismiss()
                    }
                    setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }
                    show()
                }

                true
            }


            val version = findPreference<Preference>(getString(R.string.about_version_number_key))
            version?.summary = versionNumber
            version?.isSelectable = false

            val sendFeedback = findPreference<Preference>(getString(R.string.key_send_feedback))
            sendFeedback?.setOnPreferenceClickListener {
                val mailLink = "mailto:info@korbinian-moser.de?" + "subject=Simple Budget feedback"

                val sendMail = Intent(Intent.ACTION_VIEW)
                val data = Uri.parse(mailLink)
                sendMail.data = data
                startActivity(sendMail)
                true
            }

            val rateApp = findPreference<Preference>(getString(R.string.about_rate_app_key))
            rateApp?.setOnPreferenceClickListener {
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
            otherApps?.setOnPreferenceClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://developer?id=Korbinian+Moser")))
                } catch (e: android.content.ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/developer?id=Korbinian+Moser")))
                }

                true
            }

            val licenses = findPreference<Preference>(getString(R.string.licenses_key))
            licenses?.setOnPreferenceClickListener {
                showLicenses()
                true
            }
        }

        override fun onDialogDismiss() {
            val symbol = pref.getString(getString(R.string.settings_key_currency),
                                currencySymbols[0])
            currency?.summary = when {
                currencySymbols.contains(symbol) -> {
                    currencyList[currencySymbols.indexOf(symbol)]
                }
                else -> symbol
            }
        }

        private fun showLicenses() {
            with(AlertDialog.Builder(requireContext())) {
                setTitle(getString(R.string.licenses))
                setPositiveButton(getString(R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                }
                setView(R.layout.licenses_layout)
                show()
            }
        }

        private fun updateWidget() {
            activity?.sendBroadcast(
                    SimpleBudgetApp.updateWidgetIntent(requireContext(),
                            requireActivity().application))
        }
    }
}
