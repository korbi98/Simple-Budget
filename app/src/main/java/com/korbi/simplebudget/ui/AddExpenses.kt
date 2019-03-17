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
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.Category
import com.korbi.simplebudget.logic.CurrencyTextWatcher
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.logic.NON_RECURRING
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormatSymbols

const val EXPENSE_INDEX = "prefill_index"
const val EXPENSE_DESC = "prefill_desc"
const val EXPENSE_COST = "prefill_cost"
const val EXPENSE_DATE = "prefill_date"
const val EXPENSE_CAT = "prefill_cat"

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
    private var noDecimal: Boolean? = null
    private var isSymbolOnLeft: Boolean? = null
    private lateinit var currencySymbol: String

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
                getString(R.string.settings_key_currency), "$")!!

        categories = db.getAllCategories()
        categories.sortBy { it.position }

        currencyEditLayout = findViewById(R.id.add_expense_input_layout)
        datePickerTextView = findViewById(R.id.datePicker)
        descriptionEditText = findViewById(R.id.descriptionInput)
        categoryGroup = findViewById(R.id.add_expense_category_group)
        categoryChips = mutableListOf()

        val actionBarLayout = layoutInflater.inflate(R.layout.custom_toolbar, null as ViewGroup?)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowCustomEnabled(true)
        actionBar?.customView = actionBarLayout


        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        updateDatePickerText()
        setupCurrencyInput()
        setupCategoryGroup()

        prefill()
    }

    fun save(@Suppress("UNUSED_PARAMETER")view: View) {

        val amountString = currencyInput.text.toString().replace(",", ".")

        when {
            currencyInput.text.isNullOrBlank() || amountString == "." || amountString == "-"
                                                                        || amountString == "-."->
                currencyEditLayout.error = getString(R.string.no_amount_warning)

            getSelectedCategory() == null -> {
                Toast.makeText(this, R.string.no_category_warning, Toast.LENGTH_LONG).show()
            }

            (this::expenseToUpdate.isInitialized) -> { // update existing expense
                val amount = when (noDecimal) {
                    false, null -> (amountString.toFloat() * -100).toInt()
                    true -> amountString.toInt()
                }
                expenseToUpdate.description = descriptionEditText.text.toString()
                expenseToUpdate.cost = amount
                expenseToUpdate.date = expenseDate
                expenseToUpdate.category = getSelectedCategory()!!
                db.updateExpense(expenseToUpdate)
                setResult(1)
                finish()
            }

            else -> { // save new expense

                val amount = when (noDecimal) {
                    false, null -> (amountString.toFloat() * -100).toInt()
                    true -> amountString.toInt()
                }
                val id = db.getLatestID()
                val expense = Expense(id, descriptionEditText.text.toString(), amount,
                        expenseDate, getSelectedCategory()!!, NON_RECURRING)
                db.addExpense(expense)
                finish()
            }
        }
    }

    fun setDate(@Suppress("UNUSED_PARAMETER")view: View) {
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

    fun cancel(@Suppress("UNUSED_PARAMETER")view: View) {
        finish()
    }

    private fun prefill() {

        val bundle: Bundle? = intent.extras
        val index = bundle?.getInt(EXPENSE_INDEX, 0)

        if (index != null && index != 0) {
            expenseToUpdate = Expense(index, bundle.getString(EXPENSE_DESC)!!,
                                            bundle.getInt(EXPENSE_COST),
                                            LocalDate.parse(
                                                    bundle.getString(EXPENSE_DATE), dateFormatter),
                                            db.getCategoryById(bundle.getInt(EXPENSE_CAT)), NON_RECURRING)

            val multiplier = when (noDecimal) {
                true -> -1
                false, null -> -100
            }

            currencyInput.setText(SimpleBudgetApp.decimalFormat.format(expenseToUpdate.cost.
                                                            toFloat()/multiplier).toString())
            descriptionEditText.setText(expenseToUpdate.description)
            datePickerTextView.setText(bundle.getString(EXPENSE_DATE))
            categoryChips[expenseToUpdate.category.position].isChecked = true
        }
    }

    private fun setupCurrencyInput() {
        currencyInput = findViewById(R.id.currencyInput)

        val inputLayout = findViewById<TextInputLayout>(R.id.add_expense_input_layout)
        inputLayout.hint = getString(R.string.amount_input_hint) + " $currencySymbol"
        val separator = DecimalFormatSymbols.getInstance().decimalSeparator.toString()
        val forbiddenSeparator = when (separator) {
            "," -> "."
            else -> ","
        }

        currencyInput.addTextChangedListener(CurrencyTextWatcher(currencyInput, inputLayout,
                separator, forbiddenSeparator, null, true))

        currencyInput.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                save(v)
                true
            } else {
                false
            }
        }
    }

    private fun setupCategoryGroup() {
        val iconIdArray: TypedArray = SimpleBudgetApp.res.obtainTypedArray(R.array.category_icons)

        for (cat in categories) {
            val chip = Chip(this)
            chip.text = cat.name
            chip.isClickable = true
            chip.isCheckable = true
            chip.chipBackgroundColor = ContextCompat.getColorStateList(this,
                    R.color.custom_choice_chip_selector)
            chip.setChipIconTintResource(R.color.text_color_white)
            chip.setChipIconResource(iconIdArray.getResourceId(cat.icon ,-1))
            chip.setCheckedIconResource(iconIdArray.getResourceId(cat.icon ,-1))
            chip.setOnCheckedChangeListener { _, isChecked ->
                chip.isChipIconVisible = !isChecked
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
}
