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


import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import org.threeten.bp.*
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.IsoFields


class DateHelper {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

    companion object {

        private var instance: DateHelper? = null
        private var db = DBhandler.getInstance()

        fun createInstance() {
            if (instance == null) {
                instance = DateHelper()
            }
        }

        fun getInstance(): DateHelper {
            return instance!!
        }
    }

    fun getWeeks(): MutableList<Array<LocalDate>> {

        val weeks = mutableListOf<Array<LocalDate>>()

        var date2 = LocalDate.now().with( DayOfWeek.MONDAY )
        val date1 = db.getOldestDate()

        while (date2.isAfter(date1.minusWeeks(1))) {
            Log.d("test", "test")
            val firstDayOfWeek = date2.with( DayOfWeek.MONDAY )
            val lastDayOfWeek = date2.with( DayOfWeek.SUNDAY)
            weeks.add(arrayOf(firstDayOfWeek, lastDayOfWeek))

            date2 = date2.minusWeeks(1)
        }
        return weeks
    }

    fun getMonths(): MutableList<YearMonth> {

        val months = mutableListOf<YearMonth>()

        var date2 = YearMonth.from(LocalDate.now())
        val date1 = YearMonth.from(db.getOldestDate())

        while (date2.isAfter(date1.minusMonths(1))) {
            months.add(date2)
            date2 = date2.minusMonths(1)
        }
        return months
    }

    fun getQuarters(): MutableList<Array<Int>> {
        val quarterList = mutableListOf<Array<Int>>()

        var date2 = YearMonth.from(LocalDate.now())
        val date1 = YearMonth.from(db.getOldestDate())

        when (date2.monthValue) {
            1,2,3 -> date2 = date2.withMonth(1)
            4,5,6 -> date2 = date2.withMonth(4)
            7,8,9 -> date2 = date2.withMonth(7)
            10,11,12 -> date2 = date2.withMonth(10)
        }

        while (date2.isAfter(date1.minusMonths(3))) {
            quarterList.add(arrayOf(date2.get(IsoFields.QUARTER_OF_YEAR), date1.year))
            date2 = date2.minusMonths(3)
        }
        return quarterList
    }

    fun getYears(): MutableList<Int> {
        val yearList = mutableListOf<Int>()
        val oldestDate = db.getOldestDate()
        val currentDate = LocalDate.now()

        (currentDate.year downTo oldestDate.year).forEach { yearList.add(it) }
        return yearList
    }

    fun getWeekSpinnerArray(): Array<String> {
        val weekStringArray = mutableListOf<String>(SimpleBudgetApp.res.
                                getString(R.string.this_week))
        val weekList = getWeeks()

        if (weekList.size > 1) {
            weekStringArray.add(SimpleBudgetApp.res.getString(R.string.last_week))
        }
        if (weekList.size > 2) {
            weekList.subList(2, weekList.size).forEach {
                if (!db.getExpensesByDate(it[0], it[1]).isEmpty()) {
                    val dateString = dateFormatter.format(it[0]) + " - " +
                                        dateFormatter.format(it[1])
                    weekStringArray.add(dateString)
                }
            }
        }
        return weekStringArray.toTypedArray()
    }

    fun getMonthSpinnerArray(): Array<String> {
        val monthStringArray = mutableListOf<String>(SimpleBudgetApp.res.
                                getString(R.string.this_month))
        val monthList = getMonths()
        Log.d("monthtest", monthList.size.toString())
        if (monthList.size > 1) {
            monthList.subList(1, monthList.size).forEach {
                if (!db.getExpensesByDate(it.atDay(1), it.atEndOfMonth()).isEmpty()) {
                    monthStringArray.add(it.month.toString() + " " + it.year.toString())
                }
            }
        }
        return monthStringArray.toTypedArray()
    }

    fun getQuarterSpinnerArray(): Array<String> {
        val quarterStringArray = mutableListOf<String>(SimpleBudgetApp.res.
                                getString(R.string.this_quarter))
        val quarterList = getQuarters()

        if (quarterList.size > 1) {
            quarterList.subList(1, quarterList.size).forEach {
                val dateString = when (it[0]) {
                    1 -> {
                        SimpleBudgetApp.res.getString(R.string.first_quarter)+" "+it[1].toString()
                    }
                    2 -> {
                        SimpleBudgetApp.res.getString(R.string.second_quarter)+" "+it[1].toString()
                    }
                    3 -> {
                        SimpleBudgetApp.res.getString(R.string.third_quarter)+" "+it[1].toString()
                    }
                    4 -> {
                        SimpleBudgetApp.res.getString(R.string.fourth_quarter)+" "+it[1].toString()
                    }
                    else -> "invalid quarter number"
                }
                quarterStringArray.add(dateString)
            }
        }

        return quarterStringArray.toTypedArray()
    }

    fun getYearSpinnerArray(): Array<String> {
        return getYears().map { it.toString() }.toTypedArray()
    }
}