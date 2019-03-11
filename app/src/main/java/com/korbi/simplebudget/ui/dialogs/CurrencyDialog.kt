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
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.korbi.simplebudget.R
import java.lang.ClassCastException
import java.lang.IllegalStateException

private const val NO_SELECTION = 100

class CurrencyDialog : DialogFragment() {

    private lateinit var listener: OnDismissListener
    private lateinit var chipGroup: ChipGroup
    private lateinit var currencyEditText: EditText
    private lateinit var leftSideCheckBox: CheckBox
    private lateinit var noDecimalCheckBox: CheckBox
    private lateinit var pref: SharedPreferences

    interface OnDismissListener {
        fun onDialogDismiss()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            listener = targetFragment as OnDismissListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnDismissListener")
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let {
            val builder = AlertDialog.Builder(it)

            val chipList = mutableListOf<Chip>()

            builder.setView(requireActivity().layoutInflater.inflate(R.layout.currency_dialog, null))
                    .setTitle(getString(R.string.settings_currency_dialog_titel))
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        storeCurrencySettings(chipList)
                        dialog.dismiss()
                    }

            val dialog = builder.create()
            dialog.create()

            currencyEditText = dialog.findViewById(R.id.settings_currency_custom)
            leftSideCheckBox = dialog.findViewById(R.id.settings_currency_before_amount)
            noDecimalCheckBox = dialog.findViewById(R.id.settings_currency_decimal)
            chipGroup = dialog.findViewById(R.id.settings_currency_chip_group)


            val currencySymbols = resources.getStringArray(R.array.currencies_symbols)
            val currenciesWithText = resources
                                            .getStringArray(R.array.currencies_with_names)
            pref = PreferenceManager.getDefaultSharedPreferences(context)
            val currentCurrency = pref.getString(getString(R.string.settings_key_currency), currencySymbols[0])

            for ((index, currency) in currenciesWithText.withIndex()) {
                val chip = Chip(context)
                chip.text = currency
                chip.isClickable = true
                chip.isCheckable = true
                chip.checkedIcon = ContextCompat.getDrawable(context!!, R.drawable.ic_done_white_24px)
                chipList.add(chip)
                chipGroup.addView(chip as View)
                if (index != currenciesWithText.lastIndex) {
                    chip.setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) currencyEditText.text.clear()
                    }
                } else {
                    chip.setOnCheckedChangeListener { _, isChecked ->
                        if (!isChecked) currencyEditText.text.clear()
                    }
                }
            }

            if (currencySymbols.contains(currentCurrency)) {
                chipList[currencySymbols.indexOf(currentCurrency)].isChecked = true
            } else {
                currencyEditText.setText(currentCurrency)
                chipList.last().isChecked = true
            }

            currencyEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (!s.isNullOrEmpty()) {
                        chipList.last().isChecked = true
                        checkIfEnableOK(dialog, chipList)
                    }
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int){}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            chipGroup.setOnCheckedChangeListener { _, _ ->
                checkIfEnableOK(dialog, chipList)
            }

            noDecimalCheckBox.isChecked = pref.getBoolean(getString(R.string.settings_key_currency_decimal), false)
            leftSideCheckBox.isChecked = pref.getBoolean(getString(R.string.settings_key_currency_left), false)

            checkIfEnableOK(dialog, chipList)

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun storeCurrencySettings(chipList: MutableList<Chip>) {
        val currencyList = resources.getStringArray(R.array.currencies_symbols)
        val currency = when {
            !currencyEditText.text.isEmpty() -> currencyEditText.text.toString()
            getCheckedItem(chipList) == NO_SELECTION -> currencyList[0]
            else -> {
                currencyList[getCheckedItem(chipList)]
            }
        }

        with (pref.edit()) {
            putString(getString(R.string.settings_key_currency), currency)
            putBoolean(getString(R.string.settings_key_currency_left), leftSideCheckBox.isChecked)
            putBoolean(getString(R.string.settings_key_currency_decimal),
                                        noDecimalCheckBox.isChecked)
            apply()
        }
        listener.onDialogDismiss()
    }

    private fun checkIfEnableOK(dialog: AlertDialog, chipList: MutableList<Chip>) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                (getCheckedItem(chipList) != chipList.lastIndex ||
                        !(currencyEditText.text.isEmpty())) &&
                getCheckedItem(chipList) != NO_SELECTION
    }

    private fun getCheckedItem(chipList: MutableList<Chip>): Int {

        return when {
            !chipList.none { it.isChecked } -> {
                chipList.indexOf(chipList.filter { it.isChecked } [0])
            }
            else -> NO_SELECTION
        }
    }
}