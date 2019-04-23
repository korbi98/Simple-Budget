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
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.BudgetHelper
import com.korbi.simplebudget.logic.CustomMarker
import com.korbi.simplebudget.logic.DateHelper
import com.korbi.simplebudget.utilities.MONTHLY_INTERVAL
import com.korbi.simplebudget.utilities.WEEKLY_INTERVAL
import kotlinx.android.synthetic.main.chart_markerview.view.*
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.Year
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.WeekFields
import java.util.*
import kotlin.math.round

class TotalBudgetChart(context: Context, attr: AttributeSet) : CombinedChart(context, attr) {

    private val budgetHelper = BudgetHelper()
    private val db = DBhandler.getInstance()

    fun initChart() {
        description.isEnabled = false

        legend.apply {
            form = Legend.LegendForm.CIRCLE
            textColor = Color.WHITE
            textSize = 12f
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            isWordWrapEnabled = true
            setDrawInside(false)
        }

        isHighlightFullBarEnabled = false
        drawOrder = arrayOf(DrawOrder.BAR, DrawOrder.LINE)

        xAxis.textColor = Color.WHITE
        xAxis.textSize = 12f

        axisRight.setDrawLabels(false)
        axisRight.axisMinimum = 0f
        axisLeft.textColor = Color.WHITE
        axisLeft.textSize = 12f
        axisLeft.axisMinimum = 0f
        axisLeft.valueFormatter = IAxisValueFormatter { value, _ ->
            SimpleBudgetApp.createCurrencyStringRoundToInt(round(value).toInt())
        }

        val marker = CustomMarker(context, layout = R.layout.chart_markerview)
        marker.chartView = this
        this.marker = marker
    }

    fun setChartData() {

        val intervalString = SimpleBudgetApp.pref.getString(
                context.getString(R.string.settings_key_history_grouping), "1")
        val interval = intervalString?.toIntOrNull() ?: MONTHLY_INTERVAL

        xAxis.valueFormatter = IAxisValueFormatter { value, _ ->
            getXAxisLabels(interval).let {
                it[(value % it.size).toInt()]
            }
        }

        val mData = CombinedData()
        mData.setData(generateBarData(interval))
        mData.setData(generateBudgetLine(interval))

        data = mData

        invalidate()
    }

    private fun generateBudgetLine(interval: Int): LineData {

        val totalBudget = budgetHelper.getIntervalBudget(interval)

        val entries = mutableListOf<Entry>()

        for (i in  0..getXAxisLabels(interval).size) {
            entries.add(Entry(i.toFloat(), totalBudget.toFloat()))
        }

        val lineDataSet = LineDataSet(entries, context.getString(R.string.total_budget)).apply {
            color = ContextCompat.getColor(context, R.color.incomeColor)
            lineWidth = 3f
            isHighlightEnabled = false
            setDrawCircles(false)
            setDrawValues(false)
            setDrawHighlightIndicators(false)
        }

        return LineData(lineDataSet)
    }

    private fun generateBarData(interval: Int): BarData {

        val entries = mutableListOf<BarEntry>()

        for ((i, value) in getTotalExpensesByInterval(interval).withIndex()) {
            entries.add(BarEntry(i.toFloat(), -value))
        }

        val barDataSet = BarDataSet(entries, context.getString(R.string.dashboard_expenses)).apply {
            axisDependency = YAxis.AxisDependency.LEFT
            color = ContextCompat.getColor(context, R.color.expenseColor)
            valueTextColor = Color.WHITE
            setDrawValues(false)
            highLightColor  = ContextCompat.getColor(context, R.color.expenseColor)
        }

        val barData = BarData()
        barData.addDataSet(barDataSet)
        barData.barWidth = 0.9f

        return barData
    }

    private fun getXAxisLabels(interval: Int): List<String> {
        val labels = mutableListOf<String>()

        when (interval) {
            WEEKLY_INTERVAL -> {
                for (i in 25 downTo 0) {
                    val weekFields = WeekFields.of(Locale.getDefault())
                    labels.add(LocalDate.now().minusWeeks(i.toLong()).
                            get(weekFields.weekOfWeekBasedYear()).toString())
                }
            }
            else -> {
                for (i in 11 downTo 0) {

                    val dateString = YearMonth.now().minusMonths(i.toLong()).
                            month.getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())

                    labels.add(dateString)
                }
            }
        }
        return labels
    }

    private fun getTotalExpensesByInterval(interval: Int): List<Float> {
        var startDate = LocalDate.now()

        var endDate = when (interval) {
            WEEKLY_INTERVAL -> LocalDate.now().minusWeeks(25)
            else -> LocalDate.now().minusMonths(11)
        }

        startDate = if (interval == WEEKLY_INTERVAL) {
            if (DateHelper.isSundayFirstDay()) {
                startDate.with(DayOfWeek.SUNDAY).plusWeeks(1)
            } else startDate.with(DayOfWeek.MONDAY).plusWeeks(1)
        } else startDate.plusMonths(1)


        val expenseList = mutableListOf<Float>()

        while (endDate.isBefore(startDate)) {
            endDate = when (interval) {
                WEEKLY_INTERVAL -> {
                    val intervalExpenses = with (DateHelper) {
                        db.getExpensesByDate(getFirstWeekDay(endDate), getLastWeekDay(endDate))
                    }
                    expenseList.add(intervalExpenses.filter {it.cost<0}.sumBy { it.cost }.toFloat())
                    endDate.plusWeeks(1)
                }
                else -> {
                    val start = YearMonth.from(endDate).atDay(1)
                    val end = YearMonth.from(endDate).atEndOfMonth()
                    val intervalExpenses = db.getExpensesByDate(start, end)
                    expenseList.add(intervalExpenses.filter {it.cost<0}.sumBy { it.cost }.toFloat())
                    endDate.plusMonths(1)
                }
            }
        }

        return expenseList
    }
}