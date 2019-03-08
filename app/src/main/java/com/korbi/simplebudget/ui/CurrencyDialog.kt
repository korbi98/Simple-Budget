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

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.korbi.simplebudget.R
import java.lang.IllegalStateException

class CurrencyDialog : DialogFragment() {

    private lateinit var currencySpinner: Spinner
    private lateinit var currencyEditText: EditText
    private lateinit var leftSideCheckBox: CheckBox
    private lateinit var noDecimalCheckBox: CheckBox
    private lateinit var pref: SharedPreferences

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.setView(requireActivity().layoutInflater.inflate(R.layout.currency_dialog, null))
                    .setTitle(getString(R.string.settings_currency_dialog_titel))
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }
                    .setPositiveButton(R.string.ok) { dialog, _ ->

                        storeCurrencySettings()
                        dialog.dismiss()
                    }

            val dialog = builder.create()
            dialog.create()

            currencySpinner = dialog.findViewById(R.id.settings_currency_spinner)
            currencyEditText = dialog.findViewById(R.id.settings_currency_custom)
            leftSideCheckBox = dialog.findViewById(R.id.settings_currency_before_amount)
            noDecimalCheckBox = dialog.findViewById(R.id.settings_currency_decimal)

            val currencySymbols = resources.getStringArray(R.array.currencies_symbols)
            pref = PreferenceManager.getDefaultSharedPreferences(context)
            val currentCurrency = pref.getString(getString(R.string.settings_key_currency), currencySymbols[0])

            if (currencySymbols.contains(currentCurrency)) {
                currencySpinner.setSelection(currencySymbols.indexOf(currentCurrency))
            } else {
                currencyEditText.setText(currentCurrency)
                currencySpinner.setSelection(
                        resources.getStringArray(R.array.currencies_with_names).lastIndex)
            }
            noDecimalCheckBox.isChecked = pref.getBoolean(getString(R.string.settings_key_currency_decimal), false)
            leftSideCheckBox.isChecked = pref.getBoolean(getString(R.string.settings_key_currency_left), false)


            currencyEditText.addTextChangedListener(object : TextWatcher {

                override fun afterTextChanged(s: Editable) {
                    if (s.length == 1) {
                        currencySpinner.setSelection(
                                resources.getStringArray(R.array.currencies_with_names).lastIndex)
                    }
                    checkIfEnableOK(dialog)
                }
                override fun beforeTextChanged(s: CharSequence?, x: Int, y: Int, z: Int) {}
                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                }
            })

            currencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(parent: AdapterView<*>?,
                                            view: View?,
                                            position: Int,
                                            id: Long) {
                    checkIfEnableOK(dialog)
                    if (position !=
                            resources.getStringArray(R.array.currencies_with_names).lastIndex){
                        currencyEditText.text.clear()
                    }
                }
            }
            checkIfEnableOK(dialog)

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun storeCurrencySettings() {

        val currency = when {
            !currencyEditText.text.isEmpty() -> currencyEditText.text.toString()
            else -> {
                val symbolList = resources.getStringArray(R.array.currencies_symbols)
                symbolList[currencySpinner.selectedItemPosition]
            }
        }

        with (pref.edit()) {
            putString(getString(R.string.settings_key_currency), currency)
            putBoolean(getString(R.string.settings_key_currency_left), leftSideCheckBox.isChecked)
            putBoolean(getString(R.string.settings_key_currency_decimal),
                                        noDecimalCheckBox.isChecked)
            apply()
        }
    }

    private fun checkIfEnableOK(dialog: AlertDialog) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                !(currencyEditText.text.isEmpty() && currencySpinner.selectedItemPosition ==
                resources.getStringArray(R.array.currencies_with_names).lastIndex)
    }
}