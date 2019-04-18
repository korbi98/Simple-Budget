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
import com.korbi.simplebudget.logic.model.Category
import com.korbi.simplebudget.ui.fragments.*
import com.korbi.simplebudget.utilities.*
import kotlinx.android.synthetic.main.budget_dialog.*
import java.text.DecimalFormatSymbols


class BudgetDialog : DialogFragment() {

    private lateinit var inputLayout: TextInputLayout
    private lateinit var intervalGroup: ChipGroup
    private lateinit var budgetInput: EditText
    private var currencySymbol: String? = "$"
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
            val dialog = AlertDialog.Builder(it).run {

                catIndex = arguments?.getInt(CAT_INDEX) ?: 0
                if (catIndex != SET_TOTAL_BUDGET) {
                    category = db.getCategoryById(catIndex)
                }

                val message = "${getString(R.string.dialog_budget_message)} " +
                        SimpleBudgetApp.createCurrencyString(0)
                if (catIndex == -100) {
                    setTitle(R.string.dialog_budget_total_title)
                    setMessage(message)
                }
                else {
                    setTitle("${getString(R.string.dialog_budget_title)} ${category.name}")
                    setMessage(message)
                }

                setNegativeButton(R.string.cancel) { dialog, _ ->
                    dialog.cancel()
                }
                setPositiveButton(R.string.ok) { dialog, _ ->
                    save()
                    dialog.dismiss()
                }
                setView(R.layout.budget_dialog)
                create()
            }
            dialog.create()
            dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = false

            currencySymbol = SimpleBudgetApp.pref.getString(
                    getString(R.string.settings_key_currency), "$")

            budgetInput = dialog.budget_dialog_input
            inputLayout = dialog.budget_dialog_input_layout
            intervalGroup = dialog.budget_dialog_group

            setupCurrencyInput(dialog)

            weeklyChip = dialog.chip_week_budget.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    monthlyChip.isChecked = !isChecked
                }
            }
            monthlyChip = dialog.chip_month_budget.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    weeklyChip.isChecked = !isChecked
                }
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

                    val noDecimal = SimpleBudgetApp.pref.getBoolean(
                            SimpleBudgetApp.res.getString(R.string.settings_key_currency_decimal), false)

                    if (noDecimal) {
                        budgetInput.setText(category.budget.toString())
                    } else {
                        val budget = category.budget.toFloat() / 100
                        val budgetStr = String.format("%.2f", budget)
                        budgetInput.setText(budgetStr)
                    }
                }

                if (category.interval == WEEKLY_INTERVAL) {
                    intervalGroup.check(R.id.chip_week_budget)
                } else intervalGroup.check(R.id.chip_month_budget)
            }

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")

    }

    private fun save() {

        val amount = budgetInput.text.toString().replace(",", ".").toFloatOrNull() ?: 0f

        val amountInt = when (noDecimal) {
            false, null -> (amount * 100).toInt()
            true -> amount.toInt()
        }

        val interval = when (intervalGroup.checkedChipId) {
            R.id.chip_week_budget -> WEEKLY_INTERVAL
            else -> MONTHLY_INTERVAL
        }

        if (catIndex == -100) {
            totalBudget = amountInt
            totalBudgetInterval = interval
            with(SimpleBudgetApp.pref.edit()) {
                putInt(getString(R.string.total_budget_key), totalBudget)
                putInt(getString(R.string.total_budget_interval_key), totalBudgetInterval)
                apply()
            }
        } else {
            category.budget = amountInt
            category.interval = interval

            db.updateCategory(category)
        }

        parentFragment?.let {
            if (it is DashboardFragment) it.updateView()
        }
    }

    private fun setupCurrencyInput(dialog: AlertDialog) {

        inputLayout.hint = getString(R.string.amount_input_hint) + " $currencySymbol"
        val separator = DecimalFormatSymbols.getInstance().decimalSeparator.toString()

        val noDecimal = SimpleBudgetApp.pref.getBoolean(
                SimpleBudgetApp.res.getString(R.string.settings_key_currency_decimal), false)

        budgetInput.addTextChangedListener(CurrencyTextWatcher(budgetInput, inputLayout,
                separator, isZeroAllowed = true, isNegativeAllowed = false,
                isCommaAllowed = noDecimal, dialog = dialog, enableOkIfEmpty = true))

    }

}