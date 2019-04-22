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

import android.content.Context
import android.transition.*
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.transition.doOnEnd
import androidx.core.view.size
import com.google.android.material.chip.ChipGroup
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.model.Expense
import com.korbi.simplebudget.utilities.*
import org.threeten.bp.LocalDate
import org.threeten.bp.Year
import org.threeten.bp.YearMonth

interface IntervalSelectionBackdropHelper {

    val mContext: Context
    val backdropLayout: LinearLayout
    val intervalChipGroup: ChipGroup
    val intervalSpinner: Spinner
    val intervalSpinnerLayout: View
    val mainLayout: FrameLayout

    fun initIntervalHelper() {
        intervalChipGroup.setOnCheckedChangeListener { _, _ ->
            SimpleBudgetApp.selectedIntervalType = getIntervalType()
            setupTimeSelectionSpinner()
        }

        intervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {parent?.setSelection(0)}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int,
                                        id: Long) {

                SimpleBudgetApp.selectedInterval = position
                onIntervalSelected()
            }
        }
    }

    fun onIntervalSelected()

    fun getInterval(): Int {
        with(intervalSpinner.selectedItemPosition) {
            return if (this < 0) 0 else this
        }
    }

    fun getIntervalType() = when (intervalChipGroup.checkedChipId) {
        R.id.chip_weekly -> WEEKLY_INTERVAL
        R.id.chip_monthly -> MONTHLY_INTERVAL
        R.id.chip_quarterly -> QUARTERLY_INTERVAL
        R.id.chip_yearly -> YEARLY_INTERVAL
        R.id.chip_all_time -> ALL_TIME
        else -> SimpleBudgetApp.selectedIntervalType
    }

    fun updateBackdropSelection() {
        intervalChipGroup.check( when (SimpleBudgetApp.selectedIntervalType) {
            WEEKLY_INTERVAL -> R.id.chip_weekly
            QUARTERLY_INTERVAL -> R.id.chip_quarterly
            YEARLY_INTERVAL -> R.id.chip_yearly
            ALL_TIME -> R.id.chip_all_time
            else -> R.id.chip_monthly
        })
    }

    fun setupTimeSelectionSpinner() {

        val intervalType = SimpleBudgetApp.selectedIntervalType
        if (intervalType != ALL_TIME) setIntervalSpinnerVisibility(true)

        val optionsArray = when (intervalType) {
            WEEKLY_INTERVAL -> DateHelper.getWeekSpinnerArray()
            QUARTERLY_INTERVAL -> DateHelper.getQuarterSpinnerArray()
            YEARLY_INTERVAL -> DateHelper.getYearSpinnerArray()
            ALL_TIME -> {
                setIntervalSpinnerVisibility(false)
                onIntervalSelected()
                Array(1){""}
            }
            else -> DateHelper.getMonthSpinnerArray()
        }

        val defaultPosition = when (intervalType) {
            WEEKLY_INTERVAL -> optionsArray.indexOf(mContext.getString(R.string.this_week))
            MONTHLY_INTERVAL -> optionsArray.indexOf(mContext.getString(R.string.this_month))
            QUARTERLY_INTERVAL -> optionsArray.indexOf(mContext.getString(R.string.this_quarter))
            YEARLY_INTERVAL -> optionsArray.indexOf(Year.now().toString())
            else -> 0
        }

        intervalSpinner.adapter = ArrayAdapter<String>(mContext,
                android.R.layout.simple_spinner_dropdown_item, optionsArray)

        if (optionsArray.getOrNull(defaultPosition) != null) {
            intervalSpinner.setSelection(defaultPosition)
        } else intervalSpinner.setSelection(0)
    }

    fun resetIntervalSelection() {
        with(DateHelper) {
            SimpleBudgetApp.selectedInterval = when(SimpleBudgetApp.selectedIntervalType) {
                WEEKLY_INTERVAL -> {
                    getWeekSpinnerArray().indexOf(mContext.getString(R.string.this_week))
                }
                MONTHLY_INTERVAL -> {
                    getMonthSpinnerArray().indexOf(mContext.getString(R.string.this_month))
                }
                QUARTERLY_INTERVAL -> {
                    getQuarterSpinnerArray().indexOf(mContext.getString(R.string.this_quarter))
                }
                YEARLY_INTERVAL -> getYearSpinnerArray().indexOf(Year.now().toString())
                else -> 0
            }
        }
    }

    fun getExpensesForInterval (intervalType: Int = SimpleBudgetApp.selectedIntervalType,
                                selectedInterval: Int
                                = SimpleBudgetApp.selectedInterval): MutableList<Expense> {
        val db = DBhandler.getInstance()
        var startDate = db.getOldestDate()
        var endDate = db.getNewestDate()

        when (intervalType) {
            WEEKLY_INTERVAL -> {
                var weeks = DateHelper.getWeeks()
                if (weeks.size > 0) {
                    weeks = weeks.subList(1, weeks.size) .filter {
                        db.getExpensesByDate(it[0], it[1]).isNotEmpty() ||
                                DateHelper.isBetween(LocalDate.now(), it[0], it[1])
                    } .toMutableList()
                }
                weeks.add(0, DateHelper.getWeeks()[0])

                val week = weeks.getOrNull(selectedInterval) ?: weeks[0]
                startDate = week[0]
                endDate = week[1]
            }
            MONTHLY_INTERVAL -> {
                var months = DateHelper.getMonths()
                if (months.size > 0) {
                    months = months.subList(1, months.size).filter {
                        db.getExpensesByDate(it.atDay(1), it.atEndOfMonth()).isNotEmpty()
                                || DateHelper.isBetween(LocalDate.now(),
                                it.atDay(1), it.atEndOfMonth())
                    } .toMutableList()
                }
                months.add(0, DateHelper.getMonths()[0])

                val month = months.getOrNull(selectedInterval) ?: months[0]
                startDate = month.atDay(1)
                endDate = month.atEndOfMonth()
            }
            QUARTERLY_INTERVAL -> {
                val quarters = DateHelper.getQuarters()
                val quarter = quarters.getOrNull(selectedInterval) ?: quarters[0]
                startDate = LocalDate.of(quarter[1], 1, 1)
                        .plusMonths(3*(quarter[0] - 1).toLong())
                endDate = YearMonth.of(quarter[1], 3).atEndOfMonth()
                        .plusMonths(3*(quarter[0] - 1).toLong())
            }
            YEARLY_INTERVAL -> {
                val years = DateHelper.getYears()
                val year = years.getOrNull(selectedInterval) ?: years[0]
                startDate = LocalDate.of(year, 1, 1)
                endDate = LocalDate.of(year, 12, 31)
            }
        }

        return db.getExpensesByDate(startDate, endDate)
    }

    fun setIntervalSpinnerVisibility(visible: Boolean) {
        TransitionManager.beginDelayedTransition(mainLayout, AutoTransition().apply { duration = 100 })
        intervalSpinnerLayout.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun hideBackdrop(onFinish: () -> Unit = {}) {

        val transitionSet = TransitionSet().apply {
            addTransition(Slide(Gravity.TOP))
            addTransition(ChangeBounds())

            doOnEnd {
                onFinish()
            }
        }

        TransitionManager.beginDelayedTransition(mainLayout, transitionSet)
        backdropLayout.visibility = View.GONE
    }

    fun showBackdrop() {

        val transitionSet = TransitionSet().apply {
            addTransition(Slide(Gravity.TOP))
            addTransition(ChangeBounds())
        }

        TransitionManager.beginDelayedTransition(mainLayout, transitionSet)
        backdropLayout.visibility = View.VISIBLE
    }

    fun getIntervalString(): String {
        intervalSpinner.setSelection(SimpleBudgetApp.selectedInterval)

        return when {
            getIntervalType() != ALL_TIME ->
                intervalSpinner.selectedItem?.toString() ?:
                intervalSpinner.getItemAtPosition(0)?.toString() ?:
                mContext.getString(R.string.all_time)
            else -> mContext.getString(R.string.all_time)
        }
    }
}