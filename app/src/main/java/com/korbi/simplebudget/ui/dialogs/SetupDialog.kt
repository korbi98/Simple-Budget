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

package com.korbi.simplebudget.ui.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.korbi.simplebudget.MainActivity
import com.korbi.simplebudget.R
import com.korbi.simplebudget.ui.fragments.DashboardFragment
import com.korbi.simplebudget.ui.fragments.MONTHLY_INTERVAL
import com.korbi.simplebudget.ui.fragments.WEEKLY_INTERVAL
import java.lang.IllegalStateException

class SetupDialog : DialogFragment(), CurrencyDialog.OnDismissListener {

    private lateinit var currencyButton: Button
    private lateinit var intervalChipGroup: ChipGroup
    private lateinit var startWeekChipGroup: ChipGroup
    private lateinit var chipMonday: Chip
    private lateinit var chipSunday: Chip
    private lateinit var chipWeekly: Chip
    private lateinit var chipMonthly: Chip
    private lateinit var pref: SharedPreferences


    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.setView(requireActivity().layoutInflater.inflate(R.layout.setup_dialog, null))
                    .setTitle(getString(R.string.setup_dialog_title))
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        storeSetup()
                        (requireActivity() as MainActivity).showFragment(DashboardFragment())
                        dialog.dismiss()
                    }

            val dialog = builder.create()
            dialog.create()
            dialog.setCancelable(false)

            currencyButton = dialog.findViewById(R.id.setup_currency_choose_currency_button)
            intervalChipGroup = dialog.findViewById(R.id.setup_interval_group)
            startWeekChipGroup = dialog.findViewById(R.id.setup_week_start_group)
            chipMonday = dialog.findViewById(R.id.chip_monday)
            chipSunday = dialog.findViewById(R.id.chip_sunday)
            chipMonthly = dialog.findViewById(R.id.chip_monthly)
            chipWeekly = dialog.findViewById(R.id.chip_weekly)

            setCurrencyText()

            currencyButton.setOnClickListener {
                val currencyDialog = CurrencyDialog()
                currencyDialog.setListener(this)
                currencyDialog.show(fragmentManager!!, "currency_dialog")
            }

            chipMonthly.setOnCheckedChangeListener { _, isChecked ->
                chipWeekly.isChecked = !isChecked
            }
            chipWeekly.setOnCheckedChangeListener { _, isChecked ->
                chipMonthly.isChecked = !isChecked
            }
            chipMonday.setOnCheckedChangeListener { _, isChecked ->
                chipSunday.isChecked = !isChecked
            }
            chipSunday.setOnCheckedChangeListener { _, isChecked ->
                chipMonday.isChecked = !isChecked
            }

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun setCurrencyText() {
        val currencySymbols = resources.getStringArray(R.array.currencies_symbols)
        val currenciesWithText = resources
                .getStringArray(R.array.currencies_with_names)
        pref = PreferenceManager.getDefaultSharedPreferences(context)
        val currentCurrency = pref.getString(getString(R.string.settings_key_currency), currencySymbols[0])

        currencyButton.text = if (currencySymbols.contains(currentCurrency)) {
            getString(R.string.choose_currency) +
                    currenciesWithText[currencySymbols.indexOf(currentCurrency)]
        } else {
            "${getString(R.string.choose_currency)}$currentCurrency"
        }
    }

    override fun onDialogDismiss() {
        setCurrencyText()
    }

    private fun storeSetup() {

        val startWithSunday = when (startWeekChipGroup.checkedChipId) {
            R.id.chip_sunday -> true
            else -> false
        }

        val interval = when (intervalChipGroup.checkedChipId) {
            R.id.chip_weekly -> WEEKLY_INTERVAL.toString()
            else -> MONTHLY_INTERVAL.toString()
        }

        with (pref.edit()) {
            putBoolean(getString(R.string.settings_key_start_week_sunday), startWithSunday)
            putString(getString(R.string.settings_key_history_grouping), interval)
            putBoolean(getString(R.string.settings_key_initial_start), false)
            apply()
        }
    }
}
