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

package com.korbi.simplebudget.ui.fragments

import android.graphics.Color
import android.icu.util.CurrencyAmount
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.korbi.simplebudget.R
import com.korbi.simplebudget.logic.Expense
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.Category
import kotlinx.android.synthetic.main.fragment_pie_chart.view.*
import java.text.DecimalFormat


class PieChartFragment : androidx.fragment.app.Fragment(),  DashboardFragment.DateSelectionListener {

    private lateinit var pieChart: PieChart
    private val colors = intArrayOf(
            Color.parseColor("#16a085"),
            Color.parseColor("#95a5a6"),
            Color.parseColor("#f9a825"),
            Color.parseColor("#d35400"),
            Color.parseColor("#8e44ad"),
            Color.parseColor("#2980b9"),
            Color.parseColor("#34495e"),
            Color.parseColor("#7f8c8d"),
            Color.parseColor("#2ecc71"))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_pie_chart, container, false)

        pieChart = rootView.dashboard_pie_chart

        return rootView
    }

    override fun onResume() {
        super.onResume()
        updateView()
    }

    private fun createPieData(expenses: MutableList<Expense>) {
        val totalExpense = expenses.filter { it.cost < 0 } .sumBy { it.cost } .toFloat()

        val pieEntries = mutableListOf<PieEntry>()
        val legendEntries = mutableListOf<LegendEntry>()
        val categories = DBhandler.getInstance().getAllCategories().filter {
            !isCategoryEmpty(it, expenses)
        }

        var other = 0f
        var otherExpenses = 0
        var colorIndex = 0

        for (category in categories) {
            val catExpenses = expenses.filter {
                it.category == category && it.cost < 0
            } .sumBy { it.cost }

            val percentage = catExpenses.toFloat() / totalExpense
            if (percentage < 0.03) {
                other += percentage
                otherExpenses += catExpenses
            } else {
                if (colorIndex > categories.lastIndex) colorIndex = 0

                val data = PieEntry(percentage)

                data.label = getCurrencyString(catExpenses)

                pieEntries.add(data)
                legendEntries.add(LegendEntry(category.name,
                                Legend.LegendForm.DEFAULT,
                                10f, 0f, null,
                                colors[colorIndex]))
                colorIndex += 1
            }
        }

        if (other > 0f) {
            val data = PieEntry(other)
            pieEntries.add(data)
            legendEntries.add(LegendEntry(getString(R.string.other),
                    Legend.LegendForm.DEFAULT,
                    10f, 0f, null,
                    colors[colorIndex]))
            if (other > 0.0249) {
                data.label = getCurrencyString(otherExpenses)
            }
        }

        val dataSet = PieDataSet(pieEntries, "").apply {
            valueTextSize = 10f
            valueTextColor = Color.WHITE
            sliceSpace = 2f
            selectionShift = 0f

        }
        dataSet.setColors(colors, 255)

        pieChart.legend.apply {
            form = Legend.LegendForm.CIRCLE
            textColor = Color.WHITE
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            isWordWrapEnabled = true
            setCustom(legendEntries)
        }

        pieChart.run {
            data = PieData(dataSet).apply {
                setValueFormatter { value, _, _, _ ->

                    val formatter = DecimalFormat("#0.0")

                    if (value < 2.5) {
                        ""
                    } else {
                        "${formatter.format(value)} %"
                    }
                }
            }

            setUsePercentValues(true)
            centerText = getString(R.string.distribution)
            setCenterTextSize(14f)
            setCenterTextColor(Color.WHITE)
            holeRadius = 45f
            setHoleColor(ContextCompat.getColor(requireContext(), R.color.gray_background))
            setTransparentCircleAlpha(0)
            description.text = ""
            isRotationEnabled = false

            invalidate()
        }
    }

    private fun isCategoryEmpty(category: Category, expenses: MutableList<Expense>): Boolean {
        return expenses.none { it.category == category && it.cost < 0}
    }

    override fun onDateSelectionChange() {
        updateView()
    }

    fun getListener(): DashboardFragment.DateSelectionListener {
        return this
    }

    fun updateView() {
        val dashboard = requireParentFragment() as DashboardFragment
        val expenses = if (dashboard.actionBarSinnerInitialized() &&
                dashboard.getInterval() != -1) {
            dashboard.getExpensesForInterval(dashboard.getIntervalType(), dashboard.getInterval())
        } else {
            dashboard.getExpensesForInterval(SimpleBudgetApp.pref.getInt(
                    getString(R.string.dashboard_time_selection_key), 1), 0)
        }
        createPieData(expenses)
    }

    private fun getCurrencyString(amount: Int): String {
        val currencyFormat = DecimalFormat("#")
        val currencySymbol = SimpleBudgetApp.pref
                .getString(getString(R.string.settings_key_currency), "$")

        val amountString = when (SimpleBudgetApp.pref
                .getBoolean(getString(R.string.settings_key_currency_decimal), false)) {
            true -> currencyFormat.format(-amount)
            false -> currencyFormat.format(-amount/100)
        }
        return when (SimpleBudgetApp.pref
                .getBoolean(getString(R.string.settings_key_currency_left), false)) {
            true -> "$currencySymbol$amountString"
            false -> "$amountString$currencySymbol"
        }
    }
}