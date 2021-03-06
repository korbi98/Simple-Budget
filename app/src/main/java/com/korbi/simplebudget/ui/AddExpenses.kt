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

import android.app.DatePickerDialog
import android.content.res.TypedArray
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.CurrencyTextWatcher
import com.korbi.simplebudget.logic.model.Category
import com.korbi.simplebudget.logic.model.Expense
import com.korbi.simplebudget.utilities.EXPENSE_INDEX
import com.korbi.simplebudget.utilities.NON_RECURRING
import com.korbi.simplebudget.utilities.createCurrencyStringForEditText
import com.korbi.simplebudget.utilities.parseCurrencyAmount
import kotlinx.android.synthetic.main.activity_add_expenses.*
import kotlinx.android.synthetic.main.custom_toolbar.*
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormatSymbols
import kotlin.math.abs
import kotlin.math.round

class AddExpenses : AppCompatActivity() {

    private lateinit var db: DBhandler
    private var expenseDate = LocalDate.now()

    private lateinit var descriptionEditText: EditText
    private lateinit var currencyInput: EditText
    private lateinit var currencyEditLayout: TextInputLayout
    private lateinit var categoryGroup: ChipGroup
    private lateinit var categoryChips: MutableList<Chip>
    private lateinit var datePickerTextView: EditText
    private lateinit var expenseToUpdate: Expense
    private lateinit var categories: MutableList<Category>
    private lateinit var incomeSwitchLayout: RelativeLayout
    private lateinit var incomeSwitch: CheckBox
    private var noDecimal = false
    private var isSymbolOnLeft = false
    private var currencySymbol: String? = "$"

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expenses)

        db = DBhandler.getInstance()
        noDecimal = SimpleBudgetApp.pref.getBoolean(
                getString(R.string.settings_key_currency_decimal), false)
        isSymbolOnLeft = SimpleBudgetApp.pref.getBoolean(
                getString(R.string.settings_key_currency_left), false)

        currencySymbol = SimpleBudgetApp.pref.getString(
                getString(R.string.settings_key_currency), "$")

        categories = db.getAllCategories()
        categories.sortBy { it.position }

        currencyEditLayout = add_expense_input_layout
        datePickerTextView = add_expense_date_picker
        datePickerTextView.inputType = 0
        datePickerTextView.setOnClickListener {
            setDate()
        }

        descriptionEditText = add_expense_description_input
        categoryGroup = add_expense_category_group
        categoryChips = mutableListOf()

        incomeSwitch = add_expense_income_switch
        incomeSwitchLayout = add_expense_income_switch_layout
        incomeSwitchLayout.visibility = if (SimpleBudgetApp.pref.getBoolean(
                        getString(R.string.settings_key_income_switch_visibility), true)) {
            View.VISIBLE
        } else {
            View.GONE
        }


        val actionBarLayout = layoutInflater.inflate(R.layout.custom_toolbar, null as ViewGroup?)
        supportActionBar?.run {
            setDisplayShowCustomEnabled(true)
            customView = actionBarLayout
        }

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        updateDatePickerText()
        setupCurrencyInput()
        setupCategoryGroup()

        prefill()

        custom_toolbar_cancel.setOnClickListener {
            finish()
        }

        custom_toolbar_save.setOnClickListener {
            save()
        }
    }

    private fun save() {

        var amount = currencyInput.text.toString().parseCurrencyAmount()

        if (incomeSwitch.isChecked) {
            amount = -abs(amount ?: 0)
        }

        when {
            amount == null || round(amount.toFloat() * 100).toInt() == 0 ->
                currencyEditLayout.error = getString(R.string.no_amount_warning)

            (this::expenseToUpdate.isInitialized) -> { // update existing expense

                getSelectedCategory()?.let {
                    expenseToUpdate.description = descriptionEditText.text.toString()
                    expenseToUpdate.cost = -amount
                    expenseToUpdate.date = expenseDate
                    expenseToUpdate.category = it
                    db.updateExpense(expenseToUpdate)
                    updateWidget()
                    setResult(1)
                    finish()
                } ?: Toast.makeText(this, R.string.no_category_warning, Toast.LENGTH_LONG).show()
            }

            else -> { // save new expense

                val id = db.getLatestID()
                getSelectedCategory()?.let {
                    val expense = Expense(id, descriptionEditText.text.toString(), -amount,
                            expenseDate, it, NON_RECURRING)
                    db.addExpense(expense)
                    updateWidget()
                    finish()
                } ?: Toast.makeText(this, R.string.no_category_warning, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setDate() {
        //LocalDate.monthValue goes from 1 to 12 while the datePicker takes values from 0 to 11
        val year = expenseDate.year
        val month = expenseDate.monthValue - 1
        val day = expenseDate.dayOfMonth

        val datePickerDialog = DatePickerDialog(this,
                DatePickerDialog.OnDateSetListener { _, sYear, sMonth, sDay ->

                    expenseDate = LocalDate.of(sYear, sMonth + 1, sDay)
                    updateDatePickerText()

                }, year, month, day)

        datePickerDialog.show()
    }

    private fun updateDatePickerText() {

        when (LocalDate.now()) {
            expenseDate -> datePickerTextView.setText(getString(R.string.today))
            expenseDate.plusDays(1) -> datePickerTextView.setText(getString(R.string.yesterday))
            else -> datePickerTextView.setText(dateFormatter.format(expenseDate))
        }
    }

    private fun prefill() {

        intent?.extras?.let {
            expenseToUpdate = db.getExpenseByID(it.getInt(EXPENSE_INDEX))

            if (incomeSwitchLayout.visibility == View.VISIBLE) {
                currencyInput.setText((abs(expenseToUpdate.cost)).createCurrencyStringForEditText())
                incomeSwitch.isChecked = (expenseToUpdate.cost > 0)
            } else {
                currencyInput.setText((-expenseToUpdate.cost).createCurrencyStringForEditText())
            }

            categoryChips[expenseToUpdate.category.position].isChecked = true
            expenseDate = expenseToUpdate.date
            descriptionEditText.setText(expenseToUpdate.description)
            datePickerTextView.setText(expenseDate.format(dateFormatter))
        }
    }

    private fun setupCurrencyInput() {

        val inputLayout = add_expense_input_layout
        inputLayout.hint = getString(R.string.amount_input_hint) + " $currencySymbol"
        val separator = DecimalFormatSymbols.getInstance().decimalSeparator.toString()

        currencyInput = add_expense_currency_input.apply {
            addTextChangedListener(
                    CurrencyTextWatcher(this, inputLayout, separator, isCommaAllowed = noDecimal))

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    save()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun setupCategoryGroup() {
        val iconIdArray: TypedArray = SimpleBudgetApp.res.obtainTypedArray(R.array.category_icons)

        for (cat in categories) {
            val chip = Chip(this).apply {
                text = cat.name
                isClickable = true
                isCheckable = true
                chipBackgroundColor = ContextCompat.getColorStateList(context,
                        R.color.custom_choice_chip_selector)
                setChipIconTintResource(R.color.text_color_white)
                setChipIconResource(iconIdArray.getResourceId(cat.icon ,-1))
                setCheckedIconResource(iconIdArray.getResourceId(cat.icon ,-1))
                setOnCheckedChangeListener { _, isChecked ->
                    isChipIconVisible = !isChecked
                }
            }

            categoryGroup.addView(chip)
            categoryChips.add(chip)
        }
        iconIdArray.recycle()
        categoryChips[0].isChecked = true
    }

    private fun getSelectedCategory(): Category? {

        return when {
            !categoryChips.none { it.isChecked } -> {
                val position = categoryChips.indexOf(categoryChips.find { it.isChecked })
                categories[position]
            }
            else -> null
        }
    }

    private fun updateWidget() {
        sendBroadcast(SimpleBudgetApp.updateWidgetIntent(this, application))
    }
}
