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
import com.korbi.simplebudget.database.DBhandler
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.Year
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.IsoFields
import java.util.*


object DateHelper {

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

    private var db = DBhandler.getInstance()


    fun getWeeks(): MutableList<Array<LocalDate>> {

        val weeks = mutableListOf<Array<LocalDate>>()

        var date2 = when {
            LocalDate.now().isBefore(db.getNewestDate()) -> db.getNewestDate().with (DayOfWeek.MONDAY )
            else -> LocalDate.now().with( DayOfWeek.MONDAY )
        }
        var date1 = db.getOldestDate()

        val startOnSunday = SimpleBudgetApp.pref.getBoolean(
                SimpleBudgetApp.res.getString(R.string.settings_key_start_week_sunday), false)

        if (startOnSunday) date1 = date1.minusWeeks(1)

        do {

            val firstDayOfWeek = when (startOnSunday) {
                false-> date2.with( DayOfWeek.MONDAY )
                true -> date2.with( DayOfWeek.SUNDAY )
            }
            val lastDayOfWeek = when (startOnSunday) {
                false -> date2.with( DayOfWeek.SUNDAY )
                true -> date2.with( DayOfWeek.SATURDAY ).plusWeeks(1)
            }

            if (db.getExpensesByDate(firstDayOfWeek, lastDayOfWeek).isNotEmpty()) {
                weeks.add(arrayOf(firstDayOfWeek, lastDayOfWeek))
            }

            date2 = date2.minusWeeks(1)
        }
        while (date2.isAfter(date1.minusWeeks(1)))
        return weeks
    }

    fun getMonths(): MutableList<YearMonth> {

        val months = mutableListOf<YearMonth>()

        var date2 = YearMonth.from(db.getNewestDate())
        val date1 = YearMonth.from(db.getOldestDate())

        do {
            if (db.getExpensesByDate(date2.atDay(1), date2.atEndOfMonth()).isNotEmpty()) {
                months.add(date2)
            }
            date2 = date2.minusMonths(1)
        }
        while (date2.isAfter(date1.minusMonths(1)))
        return months
    }

    fun getQuarters(): MutableList<Array<Int>> {
        val quarterList = mutableListOf<Array<Int>>()
        var date2 = YearMonth.from(db.getNewestDate())
        val date1 = YearMonth.from(db.getOldestDate())

        when (date2.monthValue) {
            1,2,3 -> date2 = date2.withMonth(1)
            4,5,6 -> date2 = date2.withMonth(4)
            7,8,9 -> date2 = date2.withMonth(7)
            10,11,12 -> date2 = date2.withMonth(10)
        }
        do {
            quarterList.add(arrayOf(date2.get(IsoFields.QUARTER_OF_YEAR), date2.year))
            date2 = date2.minusMonths(3)
        }
        while (date2.isAfter(date1.minusMonths(3)))
        return quarterList
    }

    fun getYears(): MutableList<Int> {
        val yearList = mutableListOf<Int>()
        val oldestDate = db.getOldestDate()
        val currentDate = db.getNewestDate()

        (currentDate.year downTo oldestDate.year).forEach { yearList.add(it) }
        return yearList
    }

    fun getWeekSpinnerArray(): Array<String> {
        val weekStringArray = mutableListOf<String>()
        val weekList = getWeeks()
        val now = LocalDate.now()

        weekList.forEach {
            if (isBetween(now, it[0], it[1])) {
                weekStringArray.add(SimpleBudgetApp.res.getString(R.string.this_week))
            }
            else if (!db.getExpensesByDate(it[0], it[1]).isEmpty()) {
                if (isBetween(now.minusWeeks(1), it[0], it[1])) {
                    weekStringArray.add(SimpleBudgetApp.res.getString(R.string.last_week))
                } else {
                    val dateString = dateFormatter.format(it[0]) + " - " +
                            dateFormatter.format(it[1])
                    weekStringArray.add(dateString)
                }
            }
        }

        return weekStringArray.toTypedArray()
    }

    fun getMonthSpinnerArray(): Array<String> {
        val monthStringArray = mutableListOf<String>()
        val monthList = getMonths()

        monthList.forEach {
            if (isBetween(LocalDate.now(), it.atDay(1), it.atEndOfMonth())) {
                monthStringArray.add(SimpleBudgetApp.res.getString(R.string.this_month))
            }
             else if (!db.getExpensesByDate(it.atDay(1), it.atEndOfMonth()).isEmpty()) {
                monthStringArray.add(
                        it.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " +
                                it.year.toString())
            }
        }

        return monthStringArray.toTypedArray()
    }

    fun getQuarterSpinnerArray(): Array<String> {
        val quarterStringArray = mutableListOf<String>()
        val quarterList = getQuarters()

        quarterList.forEach {
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
                else -> "invalid number"
            }
            quarterStringArray.add(dateString)
        }

        return quarterStringArray.map {
            if (it == currentQuarterString()) {
                SimpleBudgetApp.res.getString(R.string.this_quarter)
            } else it
        }.toTypedArray()
    }

    fun getYearSpinnerArray(): Array<String> {
        return getYears().map { it.toString() }.toTypedArray()
    }

    private fun currentQuarterString(): String {
        return when (YearMonth.now().monthValue) {
            4,5,6 -> {
                SimpleBudgetApp.res.getString(R.string.second_quarter)+" "+ Year.now().toString()
            }
            7,8,9 -> {
                SimpleBudgetApp.res.getString(R.string.third_quarter)+" "+ Year.now().toString()
            }
            10,11,12 -> {
                SimpleBudgetApp.res.getString(R.string.fourth_quarter)+" "+ Year.now().toString()
            }
            else -> SimpleBudgetApp.res.getString(R.string.first_quarter)+" "+ Year.now().toString()
        }
    }

    fun isBetween(now: LocalDate, start: LocalDate, end: LocalDate): Boolean {
        return now.isAfter(start.minusDays(1)) &&
                now.isBefore(end.plusDays(1))
    }
}