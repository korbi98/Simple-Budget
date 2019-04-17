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

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.logic.adapters.IncomeAdapter
import com.korbi.simplebudget.ui.dialogs.AddEditRecurrentEntryDialog
import com.korbi.simplebudget.utilities.*
import kotlinx.android.synthetic.main.activity_regular_income_manager.*

class IncomeManager : AppCompatActivity(), AddEditRecurrentEntryDialog.OnSaveListener,
                                                            IncomeAdapter.OnEditListener {

    private lateinit var incomeRecyclerView: RecyclerView
    private lateinit var incomeAdapter: IncomeAdapter
    private val db = DBhandler.getInstance()
    private lateinit var incomeList: MutableList<Expense>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_regular_income_manager)
        setTitle(R.string.income_manager_titel)

        incomeList = db.getRecurringExpenses()
        incomeAdapter = IncomeAdapter(incomeList, this)

        incomeRecyclerView = income_manager_recycler.apply {
            layoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
            adapter = incomeAdapter
            setHasFixedSize(true)
        }

        checkIfShowEmptyMessage()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSave(income: Expense, oldIncome: Expense?) {
        if (oldIncome != null) {
            db.updateExpense(income)
            incomeList[incomeList.indexOf(oldIncome)] = income
            incomeAdapter.notifyItemChanged(incomeList.indexOf(income))
        } else {
            db.addExpense(income)
            incomeList.add(income)
            incomeAdapter.notifyItemInserted(incomeList.indexOf(income))
        }

        checkIfShowEmptyMessage()
    }

    override fun onDelete(income: Expense) {
        with(AlertDialog.Builder(this)) {
            setTitle(R.string.delete_regular_income_expense)
            setMessage(R.string.delete_regular_income_expense_message)
            setNeutralButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            setNegativeButton(R.string.delete_all) { dialog, _ ->
                incomeList.remove(income)
                incomeAdapter.notifyDataSetChanged()
                db.deleteRecurringEntry(income)
                checkIfShowEmptyMessage()
                dialog.dismiss()
            }
            setPositiveButton(R.string.stop_in_future) { dialog, _ ->
                incomeList.remove(income)
                incomeAdapter.notifyDataSetChanged()
                db.convertToNonRecurring(income)
                checkIfShowEmptyMessage()
                dialog.dismiss()
            }
            show()
        }
    }

    override fun onEdit(income: Expense) {

        AddEditRecurrentEntryDialog().run {
            arguments = Bundle().apply { putInt(INCOME_INDEX, income.id) }
            show(supportFragmentManager, "addEditRecurrentEntryDialog")
        }
    }

    private fun checkIfShowEmptyMessage() {
        incomeRecyclerView.visibility = when {
            incomeList.isEmpty() -> View.GONE
            else -> View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.category_and_income_manager_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_category_and_income_manager_add -> {
                AddEditRecurrentEntryDialog().show(supportFragmentManager, "recurrentEntryDialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
