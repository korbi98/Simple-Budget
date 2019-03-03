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
import android.text.InputFilter
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import com.korbi.simplebudget.R
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.adapters.CategoryAdapter
import com.korbi.simplebudget.logic.Expense
import java.util.*
import com.korbi.simplebudget.logic.InputFilterDecimal
import java.text.DecimalFormat
import java.text.SimpleDateFormat

const val EXPENSE_INDEX = "prefill_index"
const val EXPENSE_DESC = "prefill_desc"
const val EXPENSE_COST = "prefill_cost"
const val EXPENSE_DATE = "prefill_date"
const val EXPENSE_CAT = "prefill_cat"

class AddExpenses : AppCompatActivity() {

    private lateinit var db: DBhandler
    private val calendar = Calendar.getInstance()

    private lateinit var descriptionEditText: EditText
    private lateinit var amountEditText: EditText
    private lateinit var categoryGrid: androidx.recyclerview.widget.RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var datePickerTextView: EditText
    private lateinit var expenseToUpdate: Expense

    private val dateFormatter = SimpleDateFormat("dd.MM.yy", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expenses)

        db = DBhandler.getInstance()

        datePickerTextView = findViewById(R.id.datePicker)
        amountEditText = findViewById(R.id.currencyInput)
        amountEditText.filters = arrayOf<InputFilter>(InputFilterDecimal(2))
        amountEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                save(v)
                true
            } else {
                false
            }
        }

        descriptionEditText = findViewById(R.id.descriptionInput)
        categoryGrid = findViewById(R.id.categoryChooser)
        categoryGrid.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 2)

        val actionBarLayout = layoutInflater.inflate(R.layout.custom_toolbar, null as ViewGroup?)
        val actionBar = supportActionBar
        actionBar?.setDisplayShowCustomEnabled(true)
        actionBar?.customView = actionBarLayout

        categoryAdapter = CategoryAdapter(db.getAllCategories())
        categoryGrid.adapter = categoryAdapter
        categoryAdapter.setSelectedCategory(db.getAllCategories()[0])

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        updateDatePickerText()

        prefill()
    }

    fun save(@Suppress("UNUSED_PARAMETER")view: View) {

        when {
            amountEditText.text.toString().isEmpty() ->
                Toast.makeText(this, R.string.no_amount_warning, Toast.LENGTH_LONG).show()

            categoryAdapter.getSelectedCategory() == null ->
                Toast.makeText(this, R.string.no_category_warning, Toast.LENGTH_LONG).show()

            (this::expenseToUpdate.isInitialized) -> { // update existing expense
                expenseToUpdate.description = descriptionEditText.text.toString()
                expenseToUpdate.cost = (amountEditText.text.toString().toFloat() * -100).toInt()
                expenseToUpdate.date = calendar.time
                expenseToUpdate.category = categoryAdapter.getSelectedCategory()!!
                db.updateExpense(expenseToUpdate)
                setResult(1)
                finish()
            }

            else -> { // save new expense

                val id = db.getLatestID()
                val amount = amountEditText.text.toString().toFloat() * -100 // store value in cents negative, because positive numbers are threaded as expenses
                val expense = Expense(id, descriptionEditText.text.toString(), amount.toInt(),
                        calendar.time, categoryAdapter.getSelectedCategory()!!)
                db.addExpense(expense)
                finish()
            }
        }
    }

    fun setDate(@Suppress("UNUSED_PARAMETER")view: View) {
        val cal = Calendar.getInstance()

        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this,
                DatePickerDialog.OnDateSetListener { _, sYear, sMonth, sDay ->
                    val calTest = Calendar.getInstance()
                    calTest.set(Calendar.YEAR, sYear)
                    calTest.set(Calendar.MONTH, sMonth)
                    calTest.set(Calendar.DATE, sDay)

                    calendar.time = calTest.time
                    updateDatePickerText()

                }, year, month, day)

        datePickerDialog.show()
    }

    private fun updateDatePickerText() {
        val compareCal = Calendar.getInstance()
        compareCal.add(Calendar.DAY_OF_YEAR, -1)
        if (DateUtils.isToday(calendar.timeInMillis)) {
            datePickerTextView.setText(getString(R.string.today))
        }
        else if (compareCal.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    compareCal.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)) {
            datePickerTextView.setText(getString(R.string.yesterday))
        } else {
            datePickerTextView.setText(dateFormatter.format(calendar.time))
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
        val decimalFormat = DecimalFormat("#.00")
        val bundle: Bundle? = intent.extras
        val index = bundle?.getInt(EXPENSE_INDEX, 0)

        if (index != null && index != 0) {
            expenseToUpdate = Expense(index, bundle.getString(EXPENSE_DESC)!!,
                                            bundle.getInt(EXPENSE_COST),
                                            dateFormatter.parse(bundle.getString(EXPENSE_DATE)),
                                            bundle.getString(EXPENSE_CAT)!!)

            amountEditText.setText(decimalFormat.format(expenseToUpdate.cost.
                                                                        toFloat()/-100).toString())
            descriptionEditText.setText(expenseToUpdate.description)
            datePickerTextView.setText(bundle.getString(EXPENSE_DATE))
            categoryAdapter.setSelectedCategory(expenseToUpdate.category)
        }
    }
}
