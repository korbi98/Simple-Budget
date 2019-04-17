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

import android.content.Context
import android.util.AttributeSet
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.BudgetHelper
import com.korbi.simplebudget.logic.model.Expense

class BudgetSpiderChart(context: Context, attr: AttributeSet): RadarChart(context, attr) {

    private val db = DBhandler.getInstance()
    private val budgetHelper = BudgetHelper()

    fun initChart() {

    }

    fun setChartData(expenses: MutableList<Expense>, interval: Int) {

        budgetHelper.expenses = expenses

        val categories = db.getAllCategories()

        val budgetEntries = mutableListOf<RadarEntry>()
        val categoryExpenseEntries = mutableListOf<RadarEntry>()

        val categoryNameList = categories.map { it.name }

        xAxis.valueFormatter = IAxisValueFormatter { value, _ ->
            categoryNameList[(value % categories.size).toInt()]
        }

        categories.forEach {
            categoryExpenseEntries.add(RadarEntry(budgetHelper.getCategoryExpenses(it).toFloat()))
            budgetEntries.add(RadarEntry(budgetHelper.getIntervalBudget(interval, it).toFloat()))
        }

        val budgetSet = RadarDataSet(budgetEntries, "Budget")
        val expenseSet = RadarDataSet(categoryExpenseEntries, "expenses")

        val spiderData = RadarData(listOf(budgetSet, expenseSet))

        data = spiderData
        invalidate()
    }

}