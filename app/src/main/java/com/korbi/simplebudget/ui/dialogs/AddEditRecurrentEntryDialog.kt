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
import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.*
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.lang.ClassCastException
import java.text.DecimalFormatSymbols
import kotlin.math.round

const val INCOME_INDEX = "id"

class AddEditRecurrentEntryDialog : DialogFragment() {

    private var intervalDate = LocalDate.now()

    private lateinit var currencyInput: EditText
    private lateinit var inputLayout: TextInputLayout
    private lateinit var categorySpinner: Spinner
    private lateinit var intervalGroup: ChipGroup
    private lateinit var intervalDateInput: EditText
    private lateinit var descriptionInput: EditText
    private lateinit var currencySymbol: String
    private lateinit var monthlyChip: Chip
    private lateinit var weeklyChip: Chip
    private lateinit var listener: OnSaveListener
    private var noDecimal: Boolean? = null
    private val db = DBhandler.getInstance()
    private var prefillIncome: Expense? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

    interface OnSaveListener {
        fun onSave(income: Expense, oldIncome: Expense?)
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        try {
            listener = activity as OnSaveListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement OnSaveListener")
        }

        noDecimal = SimpleBudgetApp.pref.getBoolean(
                getString(R.string.settings_key_currency_decimal), false)

        return activity?.let {
            val builder = AlertDialog.Builder(it)

            currencySymbol = SimpleBudgetApp.pref.getString(
                    getString(R.string.settings_key_currency), "$")!!

            builder.setView(requireActivity().layoutInflater.inflate(R.layout.income_manager_add_dialog, null))
                    .setTitle(getString(R.string.add_recurrent_entry_title))
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        save()
                        dialog.dismiss()
                    }

            val dialog = builder.create()
            dialog.create()

            currencyInput = dialog.findViewById(R.id.income_manager_dialog_amount_edit)!!
            inputLayout = dialog.findViewById(R.id.income_manager_input_layout)!!
            categorySpinner = dialog.findViewById(R.id.income_manager_dialog_category_spinner)!!
            intervalGroup = dialog.findViewById(R.id.income_manager_dialog_interval_group)!!
            intervalDateInput =
                    dialog.findViewById(R.id.income_manager_dialog_interval_start_from_edit)!!
            intervalDateInput.inputType = 0
            descriptionInput = dialog.findViewById(R.id.income_manager_dialog_description)!!

            dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = false

            setupCurrencyInput(dialog)
            setupCategorySpinner()
            setupStartDateInput()

            weeklyChip = dialog.findViewById(R.id.chip_weekly)!!
            monthlyChip = dialog.findViewById(R.id.chip_monthly)!!
            weeklyChip.setOnCheckedChangeListener { _, isChecked ->
                monthlyChip.isChecked = !isChecked
            }
            monthlyChip.setOnCheckedChangeListener { _, isChecked ->
                weeklyChip.isChecked = !isChecked
            }

            intervalGroup.check(R.id.chip_monthly)

            prefill()

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")

    }

    private fun prefill() {
        if (arguments?.getInt(INCOME_INDEX) != null) {
            Log.d("incomeprefill", arguments!!.getInt(INCOME_INDEX).toString())
            prefillIncome = db.getExpenseByID(arguments!!.getInt(INCOME_INDEX))
            currencyInput.setText(SimpleBudgetApp.createCurrencyString(prefillIncome!!.cost))
            categorySpinner.setSelection(prefillIncome!!.category.position)
            when (prefillIncome!!.interval) {
                WEEKLY_ROOT -> weeklyChip.isChecked = true
            }
            intervalDate = prefillIncome!!.date
            updateDatePickerText()
            descriptionInput.setText(prefillIncome!!.description)
        }
    }

    private fun save() {

        val amountString = currencyInput.text.toString().replace(",", ".")
        val id = when (prefillIncome) {
            null -> db.getLatestID()
            else -> prefillIncome!!.id
        }
        val name = descriptionInput.text.toString()
        val amount = when (noDecimal) {
            false, null -> round(amountString.toFloat() * 100).toInt()
            true -> amountString.toInt()
        }
        val category = when {
            !db.getAllCategories().none { it.position == categorySpinner.selectedItemPosition } -> {
                db.getAllCategories().find { it.position == categorySpinner.selectedItemPosition }
            }
            else -> db.getAllCategories()[0]
        }
        val interval = when (intervalGroup.checkedChipId) {
            R.id.chip_weekly -> WEEKLY_ROOT
            else -> MONTHLY_ROOT
        }

        val recurrentEntry = Expense(id, name, amount, intervalDate, category!!, interval)
        listener.onSave(recurrentEntry, prefillIncome)
    }

    private fun setupCurrencyInput(dialog: AlertDialog) {

        inputLayout.hint = getString(R.string.amount_input_hint) + " $currencySymbol"
        val separator = DecimalFormatSymbols.getInstance().decimalSeparator.toString()
        val forbiddenSeparator = when (separator) {
            "," -> "."
            else -> ","
        }

        currencyInput.addTextChangedListener(CurrencyTextWatcher(currencyInput, inputLayout,
                separator, forbiddenSeparator, dialog, true))

    }

    private fun setupCategorySpinner() {
        val categories = db.getAllCategories()
        categories.sortBy { it.position }

        categorySpinner.adapter = ArrayAdapter<String>(context!!,
                android.R.layout.simple_spinner_dropdown_item, categories.map { it.name })
    }

    private fun setupStartDateInput() {
        intervalDateInput.setOnClickListener {
            val year = intervalDate.year
            val month = intervalDate.monthValue - 1
            val day = intervalDate.dayOfMonth

            val datePickerDialog = DatePickerDialog(context!!,
                    DatePickerDialog.OnDateSetListener { _, sYear, sMonth, sDay ->

                        intervalDate = LocalDate.of(sYear, sMonth + 1, sDay)
                        updateDatePickerText()

                    }, year, month, day)

            datePickerDialog.show()
        }
        updateDatePickerText()
    }

    private fun updateDatePickerText() {

        when (LocalDate.now()) {
            intervalDate -> intervalDateInput.setText(getString(R.string.today))
            intervalDate.plusDays(1) -> intervalDateInput.setText(getString(R.string.yesterday))
            else -> intervalDateInput.setText(dateFormatter.format(intervalDate))
        }
    }
}