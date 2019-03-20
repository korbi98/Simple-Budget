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
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.logic.NON_RECURRING
import com.korbi.simplebudget.logic.adapters.IncomeAdapter
import com.korbi.simplebudget.ui.dialogs.AddEditRecurrentEntryDialog
import com.korbi.simplebudget.ui.dialogs.INCOME_INDEX

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

        incomeRecyclerView = findViewById(R.id.income_manager_recycler)
        incomeRecyclerView.layoutManager = LinearLayoutManager(applicationContext,
                RecyclerView.VERTICAL, false)
        incomeRecyclerView.setHasFixedSize(true)
        incomeList = db.getRecurringExpenses()
        incomeAdapter = IncomeAdapter(incomeList, this)
        incomeRecyclerView.adapter = incomeAdapter

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
    }

    override fun onDelete(income: Expense) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(R.string.delete_regular_income_expense)
        builder.setMessage(R.string.delete_regular_income_expense_message)
        builder.setNeutralButton(R.string.cancel) { dialog, _ ->
            dialog.cancel()
        }
        builder.setNegativeButton(R.string.delete_all) { dialog, _ ->
            incomeList.remove(income)
            incomeAdapter.notifyDataSetChanged()
            db.deleteRecurringEntry(income)
            dialog.dismiss()
        }
        builder.setPositiveButton(R.string.stop_in_future) { dialog, _ ->
            incomeList.remove(income)
            incomeAdapter.notifyDataSetChanged()
            income.interval = NON_RECURRING
            db.updateExpense(income)
            dialog.dismiss()
        }
        builder.show()
    }

    override fun onEdit(income: Expense) {

        val dialog = AddEditRecurrentEntryDialog()
        val args = Bundle()
        args.putInt(INCOME_INDEX, income.id)
        dialog.arguments = args
        dialog.show(supportFragmentManager, "addEditRecurrentEntryDialog")
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
