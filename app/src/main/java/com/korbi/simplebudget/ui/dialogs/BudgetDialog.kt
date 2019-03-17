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
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.*
import com.korbi.simplebudget.ui.fragments.BudgetFragment
import com.korbi.simplebudget.ui.fragments.MONTHLY_INTERVAL
import com.korbi.simplebudget.ui.fragments.SET_TOTAL_BUDGET
import com.korbi.simplebudget.ui.fragments.WEEKLY_INTERVAL
import java.text.DecimalFormatSymbols


class BudgetDialog : DialogFragment() {

    private lateinit var inputLayout: TextInputLayout
    private lateinit var intervalGroup: ChipGroup
    private lateinit var budgetInput: EditText
    private lateinit var currencySymbol: String
    private lateinit var monthlyChip: Chip
    private lateinit var weeklyChip: Chip
    private lateinit var category: Category
    private var catIndex = 0
    private var totalBudget = 0
    private var totalBudgetInterval = MONTHLY_INTERVAL
    private var noDecimal: Boolean? = null
    private val db = DBhandler.getInstance()


    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        noDecimal = SimpleBudgetApp.pref.getBoolean(
                getString(R.string.settings_key_currency_decimal), false)
        totalBudget = SimpleBudgetApp.pref
                .getInt(getString(R.string.total_budget_key), 0)
        totalBudgetInterval = SimpleBudgetApp.pref
                .getInt(getString(R.string.total_budget_interval_key), MONTHLY_INTERVAL)

        return activity?.let {
            val builder = AlertDialog.Builder(it)

            currencySymbol = SimpleBudgetApp.pref.getString(
                    getString(R.string.settings_key_currency), "$")!!

            catIndex = arguments!!.getInt(CAT_INDEX)
            if (catIndex != SET_TOTAL_BUDGET) {
                category = db.getCategoryById(catIndex)
            } else {
                catIndex = -100
            }


            val message = "${getString(R.string.dialog_budget_message)} " +
                    SimpleBudgetApp.createCurrencyString(0)

            if (catIndex == -100) {
                builder.setTitle(R.string.dialog_budget_total_title)
                builder.setMessage(message)
            }
            else {
                builder.setTitle("${getString(R.string.dialog_budget_title)} ${category.name}")
                builder.setMessage(message)
            }

            builder.setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            builder.setPositiveButton(R.string.ok) { dialog, _ ->
                save()
                dialog.dismiss()
            }
            builder.setView(R.layout.budget_dialog)

            val dialog = builder.create()
            dialog.create()

            dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = false

            budgetInput = dialog.findViewById(R.id.budget_dialog_input)!!
            inputLayout = dialog.findViewById(R.id.budget_dialog_input_layout)!!
            intervalGroup = dialog.findViewById(R.id.budget_dialog_group)!!

            setupCurrencyInput(dialog)

            weeklyChip = dialog.findViewById(R.id.chip_week_budget)!!
            monthlyChip = dialog.findViewById(R.id.chip_month_budget)!!
            weeklyChip.setOnCheckedChangeListener { _, isChecked ->
                monthlyChip.isChecked = !isChecked
            }
            monthlyChip.setOnCheckedChangeListener { _, isChecked ->
                weeklyChip.isChecked = !isChecked
            }

            if (catIndex == -100) {
                if (totalBudget != 0) {
                    budgetInput.setText(SimpleBudgetApp.createCurrencyString(totalBudget))
                }

                if (totalBudgetInterval == WEEKLY_INTERVAL) {
                    intervalGroup.check(R.id.chip_week_budget)
                } else intervalGroup.check(R.id.chip_month_budget)
            }
            else {
                if (category.budget != 0) {
                    budgetInput.setText(SimpleBudgetApp.createCurrencyString(category.budget))
                }

                if (category.interval == WEEKLY_INTERVAL) {
                    intervalGroup.check(R.id.chip_week_budget)
                } else intervalGroup.check(R.id.chip_month_budget)
            }

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")

    }

    private fun save() {

        val amountString = budgetInput.text.toString().replace(",", ".")

        val amount = when (noDecimal) {
            false, null -> (amountString.toFloat() * 100).toInt()
            true -> amountString.toInt()
        }

        val interval = when (intervalGroup.checkedChipId) {
            R.id.chip_week_budget -> WEEKLY_INTERVAL
            else -> MONTHLY_INTERVAL
        }

        if (catIndex == -100) {
            totalBudget = amount
            totalBudgetInterval = interval
            with(SimpleBudgetApp.pref.edit()) {
                putInt(getString(R.string.total_budget_key), totalBudget)
                putInt(getString(R.string.total_budget_interval_key), totalBudgetInterval)
                apply()
            }
        } else {
            category.budget = amount
            category.interval = interval

            db.updateCategory(category)
        }

        (requireParentFragment() as BudgetFragment).updateView()
    }

    private fun setupCurrencyInput(dialog: AlertDialog) {

        inputLayout.hint = getString(R.string.amount_input_hint) + " $currencySymbol"
        val separator = DecimalFormatSymbols.getInstance().decimalSeparator.toString()
        val forbiddenSeparator = when (separator) {
            "," -> "."
            else -> ","
        }

        budgetInput.addTextChangedListener(CurrencyTextWatcher(budgetInput, inputLayout,
                separator, forbiddenSeparator, dialog, false))

    }

}