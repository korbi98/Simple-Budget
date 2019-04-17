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

import android.os.Bundle
import android.util.SparseBooleanArray
import androidx.core.util.set
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.model.Expense
import com.korbi.simplebudget.ui.*
import com.korbi.simplebudget.utilities.MONTHLY_INTERVAL
import com.korbi.simplebudget.utilities.WEEKLY_INTERVAL
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.TemporalAdjusters
import java.util.*

object HistoryHelper {

    private var db = DBhandler.getInstance()

    val expandedStateMap = SparseBooleanArray()
    var typeSelection = TYPE_BOTH //0 for both, 1 for expenses, 2 for income
    var dateSelection = SELECT_ALL //0 last 30 days, 1 last 90 days, 2 this year, 3 all time
    lateinit var fromDateSelection: LocalDate
    lateinit var toDateSelection: LocalDate
    var categorySelection = BooleanArray(0) //true if category selected false else

    val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

    fun getHistoryEntries(type: Int,
                          date: Int,
                          fromDate: LocalDate,
                          toDate: LocalDate,
                          categories: BooleanArray,
                          expandCurrentDate: Boolean = false): MutableList<HistoryEntry> {

        val historyEntries = mutableListOf<HistoryEntry>()

        val historyGrouping = SimpleBudgetApp.pref.getString(
                SimpleBudgetApp.res.getString(R.string.settings_key_history_grouping), "1")

        when (historyGrouping) {
            MONTHLY_INTERVAL.toString() -> {
                for ((index, month) in DateHelper.getMonths().withIndex()) {

                    val dateString =
                            month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " +
                                    month.year.toString()

                    var expenses =
                            db.getExpensesByDate(month.atDay(1), month.atEndOfMonth())
                    expenses = filterExpenses(expenses, type, date, fromDate, toDate, categories)

                    val isCurrentInterval = DateHelper.isBetween(
                            LocalDate.now(), month.atDay(1), month.atEndOfMonth())
                            && expandCurrentDate

                    if (isCurrentInterval) expandedStateMap[index] = true

                    historyEntries.add(HistoryEntry(expenses, dateString, isCurrentInterval))
                }
            }

            WEEKLY_INTERVAL.toString() -> {
                for ((index, week) in DateHelper.getWeeks().withIndex()) {

                    val dateString =
                            "${dateFormatter.format(week[0])} - ${dateFormatter.format(week[1])}"

                    var expenses = db.getExpensesByDate(week[0], week[1])
                    expenses = filterExpenses(expenses, type, date, fromDate, toDate, categories)


                    val isCurrentInterval =
                            DateHelper.isBetween(LocalDate.now(), week[0], week[1])

                    if (isCurrentInterval) expandedStateMap[index] = true

                    historyEntries.add(HistoryEntry(expenses, dateString, isCurrentInterval))
                }
            }
        }
        return historyEntries
    }

    private fun filterExpenses(expenses: MutableList<Expense>,
                               typeSelection: Int,
                               dateSelection: Int,
                               fromDate: LocalDate,
                               toDate: LocalDate,
                               categorySelection: BooleanArray) : MutableList<Expense> {

        // filter income or expense
        val typeFilteredList: List<Expense> = when (typeSelection) {
            TYPE_EXPENSE -> expenses.filter { it.cost <= 0} // Expenses
            TYPE_INCOME -> expenses.filter { it.cost > 0 } // Income
            else -> expenses
        }

        // filter date selection
        val currentDate = LocalDate.now()

        val dateFilteredList: List<Expense> = when (dateSelection) {
            SELECT_LAST30 -> {
                typeFilteredList.filter {
                    it.date.isAfter(currentDate.minusDays(31)) &&
                            it.date.isBefore(currentDate.plusDays(1))
                }
            }
            SELECT_LAST90 -> {
                typeFilteredList.filter {
                    it.date.isAfter(currentDate.minusDays(91)) &&
                            it.date.isBefore(currentDate.plusDays(1))
                }
            }
            SELECT_YEAR -> {
                typeFilteredList.filter {
                    it.date.isAfter(currentDate.with(TemporalAdjusters.firstDayOfYear()))&&
                            it.date.isBefore(currentDate.plusDays(1))
                }
            }
            SELECT_CUSTOM -> {
                typeFilteredList.filter {
                    it.date.isAfter(fromDate.minusDays(1)) &&
                            it.date.isBefore(toDate.plusDays(1))
                }
            }
            else -> typeFilteredList
        }

        val categoryFilteredList = dateFilteredList.filter {
            categorySelection[it.category.position]
        }

        return categoryFilteredList.toMutableList()
    }

    fun createPrefilledFilterSheet(): FilterBottomSheet {
        val bundle = Bundle().apply {
            putInt(TYPE_PREFILL, typeSelection)
            putInt(DATE_PREFILL, dateSelection)
            putString(FROM_DATE_PRESELECT,
                    dateFormatter.format(fromDateSelection))
            putString(TO_DATE_PRESELECT,
                    dateFormatter.format(toDateSelection))
            putBooleanArray(CATEGORY_PRESELECT, categorySelection)
        }

        return FilterBottomSheet().apply { arguments = bundle }
    }
}