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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import com.google.android.material.chip.ChipGroup
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import org.threeten.bp.LocalDate
import org.threeten.bp.Year
import org.threeten.bp.YearMonth

const val ALL_TIME = 4
const val YEARLY_INTERVAL = 3
const val QUARTERLY_INTERVAL = 2
const val MONTHLY_INTERVAL = 1
const val WEEKLY_INTERVAL = 0

interface IntervalSelectionBackdropHelper {

    val mContext: Context
    val backdropLayout: LinearLayout
    val intervalChipGroup: ChipGroup
    val intervalSpinner: Spinner
    val intervalSpinnerLayout: View
    val mainLayout: View
    var deltaY: Float


    fun initIntervalHelper() {
        intervalChipGroup.setOnCheckedChangeListener { _, _ ->
            setupTimeSelectionSpinner(getIntervalType())
        }

        intervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {parent?.setSelection(0)}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int,
                                        id: Long) {
                onIntervalSelected()
            }
        }
    }

    fun onIntervalSelected()

    fun getInterval(): Int {
        return  intervalSpinner.selectedItemPosition
    }

    fun getIntervalType(): Int {
        val default = SimpleBudgetApp.pref.getInt(
                mContext.getString(R.string.dashboard_time_selection_key), MONTHLY_INTERVAL)

        return when (intervalChipGroup.checkedChipId) {
            R.id.chip_weekly -> WEEKLY_INTERVAL
            R.id.chip_monthly -> MONTHLY_INTERVAL
            R.id.chip_quarterly -> QUARTERLY_INTERVAL
            R.id.chip_yearly -> YEARLY_INTERVAL
            R.id.chip_all_time -> ALL_TIME
            else -> default
        } .also {
            with(SimpleBudgetApp.pref.edit()) {
                putInt(SimpleBudgetApp.res.getString(R.string.dashboard_time_selection_key), it)
                apply()
            }
        }
    }

    fun selectIntervalChip() {
        val selection = SimpleBudgetApp.pref.getInt(
                mContext.getString(R.string.dashboard_time_selection_key), MONTHLY_INTERVAL)

        intervalChipGroup.check( when (selection) {
            WEEKLY_INTERVAL -> R.id.chip_weekly
            QUARTERLY_INTERVAL -> R.id.chip_quarterly
            YEARLY_INTERVAL -> R.id.chip_yearly
            ALL_TIME -> R.id.chip_all_time
            else -> R.id.chip_monthly
        })
    }

    fun setupTimeSelectionSpinner(intervalType: Int) {

        intervalSpinnerLayout.visibility = View.VISIBLE

        val optionsArray = when (intervalType) {
            WEEKLY_INTERVAL -> DateHelper.getWeekSpinnerArray()
            QUARTERLY_INTERVAL -> DateHelper.getQuarterSpinnerArray()
            YEARLY_INTERVAL -> DateHelper.getYearSpinnerArray()
            ALL_TIME -> {
                intervalSpinnerLayout.visibility = View.GONE
                onIntervalSelected()
                Array(1){""}
            }
            else -> DateHelper.getMonthSpinnerArray()
        }
        val position = when (intervalType) {
            WEEKLY_INTERVAL -> optionsArray.indexOf(mContext.getString(R.string.this_week))
            MONTHLY_INTERVAL -> optionsArray.indexOf(mContext.getString(R.string.this_month))
            QUARTERLY_INTERVAL -> optionsArray.indexOf(mContext.getString(R.string.this_quarter))
            YEARLY_INTERVAL -> optionsArray.indexOf(Year.now().toString())
            else -> 0
        }

        intervalSpinner.adapter = ArrayAdapter<String>(mContext,
                android.R.layout.simple_spinner_dropdown_item, optionsArray)

        if (position != -1) {
            intervalSpinner.setSelection(position)
        }
    }

    fun getExpensesForInterval (intervalType: Int = getIntervalType(),
                                selectedInterval: Int = getInterval()): MutableList<Expense> {
        val db = DBhandler.getInstance()
        var startDate = db.getOldestDate()
        var endDate = db.getNewestDate()

        when (intervalType) {
            WEEKLY_INTERVAL -> {
                var weeks = DateHelper.getWeeks()
                if (weeks.size > 0) {
                    weeks = weeks.subList(1, weeks.size) .filter {
                        !db.getExpensesByDate(it[0], it[1]).isEmpty() ||
                                DateHelper.isBetween(LocalDate.now(), it[0], it[1])
                    } .toMutableList()
                }
                weeks.add(0, DateHelper.getWeeks()[0])

                val week = weeks[selectedInterval]
                startDate = week[0]
                endDate = week[1]
            }
            MONTHLY_INTERVAL -> {
                var months = DateHelper.getMonths()
                if (months.size > 0) {
                    months = months.subList(1, months.size).filter {
                        !db.getExpensesByDate(it.atDay(1), it.atEndOfMonth()).isEmpty()
                                || DateHelper.isBetween(LocalDate.now(),
                                it.atDay(1), it.atEndOfMonth())
                    } .toMutableList()
                }
                months.add(0, DateHelper.getMonths()[0])

                val month = months[selectedInterval]
                startDate = month.atDay(1)
                endDate = month.atEndOfMonth()
            }
            QUARTERLY_INTERVAL -> {
                val quarter = DateHelper.getQuarters()[selectedInterval]
                startDate = LocalDate.of(quarter[1], 1, 1)
                        .plusMonths(3*(quarter[0] - 1).toLong())
                endDate = YearMonth.of(quarter[1], 3).atEndOfMonth()
                        .plusMonths(3*(quarter[0] - 1).toLong())
            }
            YEARLY_INTERVAL -> {
                startDate = LocalDate.of(DateHelper.getYears()[selectedInterval], 1, 1)
                endDate = LocalDate.of(DateHelper.getYears()[selectedInterval], 12, 31)
            }
        }

        return db.getExpensesByDate(startDate, endDate)
    }

    fun hideBackdrop(onFinish: () -> Unit = {}) {
        backdropLayout.startAnimation(TranslateAnimation(0f,0f, 0f, -deltaY).apply {
            duration = 200
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {

                }
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    backdropLayout.visibility = View.GONE
                    backdropLayout.clearAnimation()
                    onFinish()
                }
            })
            startNow()
        })

        mainLayout.startAnimation(TranslateAnimation(0f,0f, 0f, -deltaY).apply {
            duration = 200
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                }
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    mainLayout.clearAnimation()
                    onFinish()
                }
            })
        })
    }

    fun showBackdrop() {
        backdropLayout.startAnimation(TranslateAnimation(0f,0f, -deltaY, 0f).apply {
            duration = 200
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    backdropLayout.visibility = View.VISIBLE
                    backdropLayout.clearAnimation()
                }
            })
        })

        mainLayout.startAnimation(TranslateAnimation(0f,0f, 0f, deltaY).apply {
            duration = 200
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    mainLayout.clearAnimation()
                }
            })
        })
    }

    fun getIntervalString(): String {
        return when {
            getIntervalType() != ALL_TIME ->
                intervalSpinner.selectedItem?.toString() ?: mContext.getString(R.string.all_time)
            else -> mContext.getString(R.string.all_time)
        }
    }
}