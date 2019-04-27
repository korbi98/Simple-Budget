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

package com.korbi.simplebudget.logic

import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.logic.model.Category
import com.korbi.simplebudget.logic.model.Expense
import com.korbi.simplebudget.utilities.*

class BudgetHelper {

    var interval = MONTHLY_INTERVAL
    var expenses = mutableListOf<Expense>()
    var categories = mutableListOf<Category>()


    fun getCategoryExpenses(category: Category): Long {
        val categoryExpenses = expenses.filter { it.category == category && it.cost < 0}
        return -categoryExpenses.sumByLong { it.cost }
    }

    fun getBudgetText(category: Category): String {

        val budget = getIntervalBudget(interval, category)

        val onLeft = SimpleBudgetApp.pref.getBoolean(
                SimpleBudgetApp.res.getString(R.string.settings_key_currency_left), false)

        val expensesString = getCategoryExpenses(category).createCurrencyString(true, !onLeft)

        val currencySymbol = SimpleBudgetApp.pref.getString(
                SimpleBudgetApp.res.getString(R.string.settings_key_currency),
                SimpleBudgetApp.res.getStringArray(R.array.currencies_symbols)[0])

        return when {
            interval == ALL_TIME ->
                getCategoryExpenses(category).createCurrencyString(true)

            budget != 0L -> {
                val budgetStr = budget.createCurrencyString(true, onLeft)
                "$expensesString / $budgetStr"
            }

            else -> { // category has expenses but no budget set
                when (onLeft) {
                    true -> "$expensesString / -"
                    false -> "$expensesString / - $currencySymbol"
                }
            }
        }
    }

    fun getTotalBudgetText(): String {
        val totalExpenses = -expenses.filter { it.cost < 0}.sumByLong { it.cost }
        val totalBudget = getIntervalBudget(interval)

        val onLeft = SimpleBudgetApp.pref.getBoolean(
                SimpleBudgetApp.res.getString(R.string.settings_key_currency_left), false)

        val budgetString = totalExpenses.createCurrencyString(true, !onLeft)

        return when {
            interval == ALL_TIME -> totalExpenses.createCurrencyString(true)

            totalBudget != 0L -> {
                val str = totalBudget.createCurrencyString(true, onLeft)
                "$budgetString / $str"
            }
            else -> {
                SimpleBudgetApp.res.getString(R.string.no_budget_set_info)
            }
        }
    }

    fun getBudgetProgress(category: Category): Int{
        val categoryTotalSum = getCategoryExpenses(category)
        val budget = getIntervalBudget(interval, category)
        val maxAmount = categories.maxBy { getCategoryExpenses(it) }?.let {
            getCategoryExpenses(it)
        } ?: 0

        return when {
            interval == ALL_TIME -> {
                if (maxAmount != 0L) {
                    (categoryTotalSum.toFloat() / maxAmount.toFloat() * 100).toInt()
                } else 0
            }
            categoryTotalSum > 0 && budget != 0L -> {
                ((categoryTotalSum.toFloat() / budget.toFloat()) * 100).toInt()
            }
            else -> 0
        }
    }

    fun getTotalBudgetProgress(): Int {

        val totalExpenses = -expenses.filter { it.cost < 0}.sumByLong { it.cost }

        val budget = getIntervalBudget(interval)

        return when {
            interval == ALL_TIME -> 100
            totalExpenses > 0 && budget != 0L -> {
                ((totalExpenses.toFloat() / budget.toFloat())*100).toInt()
            }
            else -> 0
        }
    }

    fun getIntervalBudget(selectedInterval: Int, category: Category? = null): Long {

        val budget = category?.budget ?: SimpleBudgetApp.pref.getLong(
                SimpleBudgetApp.res.getString(R.string.total_budget_key), 0L)

        val budgetInterval= category?.interval ?: SimpleBudgetApp.pref.getInt(
                SimpleBudgetApp.res.getString(R.string.total_budget_interval_key), MONTHLY_INTERVAL)

        return when (selectedInterval) {
            ALL_TIME -> 0
            YEARLY_INTERVAL -> {
                when (budgetInterval) {
                    WEEKLY_INTERVAL -> budget * 52
                    else -> budget * 12
                }
            }
            QUARTERLY_INTERVAL -> {
                when (budgetInterval) {
                    WEEKLY_INTERVAL -> budget * 13
                    else -> budget * 4
                }
            }
            MONTHLY_INTERVAL -> {
                when (budgetInterval) {
                    WEEKLY_INTERVAL -> budget * 4
                    else -> budget
                }
            }
            else -> { // WEEKLY_INTERVAL
                when (budgetInterval) {
                    WEEKLY_INTERVAL -> budget
                    else -> {
                        (budget / 4.33f).toLong()
                    }
                }
            }
        }
    }
}