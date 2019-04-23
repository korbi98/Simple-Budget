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

package com.korbi.simplebudget.ui.charts

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.BudgetHelper
import com.korbi.simplebudget.logic.CustomMarker
import com.korbi.simplebudget.logic.model.Expense
import kotlin.math.round


class BudgetSpiderChart(context: Context, attr: AttributeSet): RadarChart(context, attr) {

    private val db = DBhandler.getInstance()
    private val budgetHelper = BudgetHelper()

    fun initChart() {

        legend.apply {
            form = Legend.LegendForm.CIRCLE
            textColor = Color.WHITE
            textSize = 12f
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            isWordWrapEnabled = true
            setDrawInside(true)
        }
        description.isEnabled = false

        xAxis.textColor = Color.WHITE
        xAxis.textSize = 12f

        yAxis.textColor = Color.WHITE
        yAxis.textSize = 8f

        yAxis.valueFormatter = IAxisValueFormatter { value, _ ->
            SimpleBudgetApp.createCurrencyStringRoundToInt(round(value).toInt())
        }

        isRotationEnabled = false

        val marker = CustomMarker(context, layout = R.layout.chart_markerview)
        marker.chartView = this
        this.marker = marker
    }

    fun setChartData(expenses: MutableList<Expense>, interval: Int) {

        budgetHelper.expenses = expenses

        val categories = db.getAllCategories().filter { it.budget != 0 }

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

        val budgetSet = RadarDataSet(budgetEntries, context.getString(R.string.budget)).apply {
            color = ContextCompat.getColor(context, R.color.incomeColor)
            fillColor = ContextCompat.getColor(context, R.color.incomeColor)
            setDrawFilled(true)
            fillAlpha = 127
            lineWidth = 2f
            setDrawHighlightIndicators(false)
            isDrawHighlightCircleEnabled = true
        }
        val expenseSet = RadarDataSet(categoryExpenseEntries,
                context.getString(R.string.dashboard_expenses)).apply {
            color = ContextCompat.getColor(context, R.color.expenseColor)
            fillColor = ContextCompat.getColor(context, R.color.expenseColor)
            setDrawFilled(true)
            fillAlpha = 127
            lineWidth = 2f
            setDrawHighlightIndicators(false)
            isDrawHighlightCircleEnabled = true
        }

        data = RadarData(listOf(budgetSet, expenseSet)).apply {
            setValueTextColor(Color.WHITE)
            setDrawValues(false)
        }

        invalidate()
    }
}