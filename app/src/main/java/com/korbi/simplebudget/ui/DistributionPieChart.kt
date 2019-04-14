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
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.Category
import com.korbi.simplebudget.logic.Expense
import java.text.DecimalFormat
import kotlin.math.absoluteValue

class DistributionPieChart(context: Context, attr: AttributeSet) : PieChart(context, attr) {

    private val colors: IntArray = intArrayOf(
            Color.parseColor("#16a085"),
            Color.parseColor("#95a5a6"),
            Color.parseColor("#f9a825"),
            Color.parseColor("#d35400"),
            Color.parseColor("#8e44ad"),
            Color.parseColor("#2980b9"),
            Color.parseColor("#34495e"),
            Color.parseColor("#7f8c8d"),
            Color.parseColor("#2ecc71"))

    fun createPieData(expenses: MutableList<Expense>, showIncome: Boolean = false) {

        fun selectExpensesOrIncome(): List<Expense> = when(showIncome){
            true -> expenses.filter { it.cost > 0 }
            false -> expenses.filter { it.cost < 0 }
        }

        val totalAmount = selectExpensesOrIncome().sumBy { it.cost } .toFloat()

        val pieEntries = mutableListOf<PieEntry>()
        val legendEntries = mutableListOf<LegendEntry>()
        val categories = DBhandler.getInstance().getAllCategories().filter {
            !isCategoryEmpty(it, expenses)
        }

        var other = 0f
        var otherExpenses = 0
        var colorIndex = 0

        for (category in categories) {
            val catExpenses = selectExpensesOrIncome().filter {
                it.category == category
            } .sumBy { it.cost }

            val percentage = catExpenses.toFloat() / totalAmount
            if (percentage < 0.03) {
                other += percentage
                otherExpenses += catExpenses
            } else {
                if (colorIndex > categories.lastIndex) colorIndex = 0

                val data = PieEntry(percentage)

                data.label = getCurrencyString(catExpenses.absoluteValue)

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
            legendEntries.add(LegendEntry(context.getString(R.string.other),
                    Legend.LegendForm.DEFAULT,
                    10f, 0f, null,
                    colors[colorIndex]))
            if (other > 0.0249) {
                data.label = getCurrencyString(otherExpenses)
            }
        }

        val dataSet = PieDataSet(pieEntries, "").apply {
            valueTextSize = 12f
            valueTextColor = Color.WHITE
            sliceSpace = 2f
            selectionShift = 0f

        }
        dataSet.setColors(colors, 255)

        this.legend.apply {
            form = Legend.LegendForm.CIRCLE
            textColor = Color.WHITE
            textSize = 12f
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            verticalAlignment = Legend.LegendVerticalAlignment.TOP
            isWordWrapEnabled = true
            setCustom(legendEntries)
        }

        this.run {
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
            centerText = context.getString(R.string.distribution)
            setCenterTextSize(14f)
            setCenterTextColor(Color.WHITE)
            holeRadius = 45f
            setHoleColor(ContextCompat.getColor(context, R.color.gray_background))
            setTransparentCircleAlpha(0)
            description.text = ""
            isRotationEnabled = false

            invalidate()
        }
    }

    private fun getCurrencyString(amount: Int): String {
        val currencyFormat = DecimalFormat("#")
        val currencySymbol = SimpleBudgetApp.pref
                .getString(context.getString(R.string.settings_key_currency), "$")

        val amountString = when (SimpleBudgetApp.pref
                .getBoolean(context.getString(R.string.settings_key_currency_decimal), false)) {
            true -> currencyFormat.format(amount)
            false -> currencyFormat.format(amount/100)
        }
        return when (SimpleBudgetApp.pref
                .getBoolean(context.getString(R.string.settings_key_currency_left), false)) {
            true -> "$currencySymbol$amountString"
            false -> "$amountString$currencySymbol"
        }
    }

    private fun isCategoryEmpty(category: Category, expenses: MutableList<Expense>): Boolean {
        return expenses.none { it.category == category && it.cost < 0}
    }
}