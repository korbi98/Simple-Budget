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
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.adapters.IncomeAdapter
import com.korbi.simplebudget.ui.dialogs.AddEditRecurrentEntryDialog

class IncomeManager : AppCompatActivity() {

    private lateinit var incomeRecyclerView: RecyclerView
    private lateinit var incomeAdapter: IncomeAdapter
    private val db = DBhandler.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_regular_income_manager)
        setTitle(R.string.income_manager_titel)

        incomeRecyclerView = findViewById(R.id.income_manager_recycler)
        incomeRecyclerView.layoutManager = LinearLayoutManager(applicationContext,
                RecyclerView.VERTICAL, false)
        incomeRecyclerView.setHasFixedSize(true)
        incomeAdapter = IncomeAdapter(db.getRecurringExpenses())
        incomeRecyclerView.adapter = incomeAdapter

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
