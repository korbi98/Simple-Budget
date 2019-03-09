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
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.Category
import com.korbi.simplebudget.logic.adapters.CategoryAdapter
import com.korbi.simplebudget.logic.Expense
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.lang.StringBuilder
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
    private lateinit var amountEditText: EditText
    private lateinit var categoryGrid: androidx.recyclerview.widget.RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var datePickerTextView: EditText
    private lateinit var expenseToUpdate: Expense
    private var noDecimal: Boolean? = null

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expenses)

        db = DBhandler.getInstance()
        noDecimal = SimpleBudgetApp.pref.getBoolean(
                getString(R.string.settings_key_currency_decimal), false)

        val currencySymbol = findViewById<TextView>(R.id.currencyIndicator)
        currencySymbol.text = SimpleBudgetApp.pref.
                getString(getString(R.string.settings_key_currency), "$")

        datePickerTextView = findViewById(R.id.datePicker)

        descriptionEditText = findViewById(R.id.descriptionInput)
        categoryGrid = findViewById(R.id.categoryChooser)
        categoryGrid.layoutManager = GridLayoutManager(this, 2)

        val actionBarLayout = layoutInflater.inflate(R.layout.custom_toolbar, null as ViewGroup?)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowCustomEnabled(true)
        actionBar?.customView = actionBarLayout

        categoryAdapter = CategoryAdapter(db.getAllCategories())
        categoryGrid.adapter = categoryAdapter
        categoryAdapter.setSelectedCategory(db.getAllCategories().filter { it.position == 0 }[0])

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        updateDatePickerText()
        setupCurrencyEditText()

        prefill()
    }

    fun save(@Suppress("UNUSED_PARAMETER")view: View) {

        val amountString = amountEditText.text.toString().replace(",", ".")
        val amount = when (noDecimal) {
            false, null -> (amountString.toFloat() * -100).toInt()
            true -> amountString.toInt()
        }

        when {
            amountEditText.text.toString().isEmpty() ->
                Toast.makeText(this, R.string.no_amount_warning, Toast.LENGTH_LONG).show()

            categoryAdapter.getSelectedCategory() == null ->
                Toast.makeText(this, R.string.no_category_warning, Toast.LENGTH_LONG).show()

            (this::expenseToUpdate.isInitialized) -> { // update existing expense
                expenseToUpdate.description = descriptionEditText.text.toString()
                expenseToUpdate.cost = amount
                expenseToUpdate.date = expenseDate
                expenseToUpdate.category = categoryAdapter.getSelectedCategory()!!
                db.updateExpense(expenseToUpdate)
                setResult(1)
                finish()
            }

            else -> { // save new expense

                val id = db.getLatestID()
                val expense = Expense(id, descriptionEditText.text.toString(), amount,
                        expenseDate, categoryAdapter.getSelectedCategory() as Category)
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

        when {
            LocalDate.now().isEqual(expenseDate) -> {
                datePickerTextView.setText(getString(R.string.today))
            }
            LocalDate.now().minusDays(1).isEqual(expenseDate) -> {
                datePickerTextView.setText(getString(R.string.yesterday))
            }
            else -> datePickerTextView.setText(dateFormatter.format(expenseDate))
        }
        datePickerTextView
    }

    fun cancel(@Suppress("UNUSED_PARAMETER")view: View) {
        finish()
    }

    override fun onStop() {
        super.onStop()
        db.close()
    }

    private fun prefill() {

        val bundle: Bundle? = intent.extras
        val index = bundle?.getInt(EXPENSE_INDEX, 0)

        if (index != null && index != 0) {
            expenseToUpdate = Expense(index, bundle.getString(EXPENSE_DESC)!!,
                                            bundle.getInt(EXPENSE_COST),
                                            LocalDate.parse(
                                                    bundle.getString(EXPENSE_DATE), dateFormatter),
                                            db.getCategoryById(bundle.getInt(EXPENSE_CAT)))

            val multiplier = when (noDecimal) {
                true -> -1
                false, null -> -100
            }

            amountEditText.setText(SimpleBudgetApp.decimalFormat.format(expenseToUpdate.cost.
                                                            toFloat()/multiplier).toString())
            descriptionEditText.setText(expenseToUpdate.description)
            datePickerTextView.setText(bundle.getString(EXPENSE_DATE))
            categoryAdapter.setSelectedCategory(expenseToUpdate.category)
        }
    }

    private fun setupCurrencyEditText() {
        amountEditText = findViewById(R.id.currencyInput)
        val separator = DecimalFormatSymbols.getInstance().decimalSeparator.toString()
        val forbiddenSeparator = when (separator) {
            "," -> "."
            else -> ","
        }

        amountEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                var hasSeparator = false
                var separatorPosition:Int? = null
                val originalString = s.toString()
                val newStringBuilder = StringBuilder()

                for ((position, char) in originalString.withIndex()) {
                    var c = char.toString()

                    if (c == separator) {
                        when (hasSeparator) {
                            false -> {
                                hasSeparator = true
                                separatorPosition = position
                            }
                            true -> c = ""
                        }
                    }
                    if (c == forbiddenSeparator || (position != 0 && c == "-") || (position > 11) ||
                            (separatorPosition != null && position > (separatorPosition + 2))) {
                        c = ""
                    }
                    newStringBuilder.append(c)
                }
                val newString = newStringBuilder.toString()
                if (newString != originalString) {
                    amountEditText.setText(newString)
                    amountEditText.setSelection(newString.length)
                }

            }


            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        amountEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                save(v)
                true
            } else {
                false
            }
        }
    }
}
