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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.logic.model.Expense
import com.korbi.simplebudget.ui.DistributionPieChart
import com.korbi.simplebudget.utilities.NON_RECURRING
import kotlinx.android.synthetic.main.fragment_distribution.view.*
import java.text.NumberFormat
import kotlin.math.absoluteValue


class DistributionFragment : androidx.fragment.app.Fragment(), StatisticFragment.DateSelectionListener {

    private lateinit var expenseChart: DistributionPieChart
    private lateinit var expenseEmptyMsg: TextView
    private lateinit var runningChargeText: TextView
    private lateinit var variableExpenseText: TextView
    private lateinit var runningChargePercentText: TextView
    private lateinit var variableExpensePercentageText: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {


        val rootView = inflater.inflate(R.layout.fragment_distribution, container, false)

        expenseChart = rootView.distribution_expense_chart
        expenseEmptyMsg = rootView.distribution_expense_empty_msg
        variableExpenseText = rootView.distribution_variable_expenses
        runningChargeText = rootView.distribution_running_charges
        variableExpensePercentageText = rootView.distribution_variable_expenses_percentage
        runningChargePercentText = rootView.distribution_running_charges_percentage

        expenseChart.initPie()

        return rootView
    }

    override fun onResume() {
        super.onResume()
        updateView()
    }

    override fun onDateSelectionChange() {
        updateView()
    }

    fun updateView() {
        val dashboard = requireParentFragment() as StatisticFragment
        with(dashboard) {
            val expenses = if (getInterval() != -1) {
                getExpensesForInterval()
            } else {
                getExpensesForInterval(SimpleBudgetApp.pref.getInt(
                        getString(R.string.selected_interval_type_key), 1), 0)
            }

            updatePieChart(expenses)
            updateRunningCharges(expenses)
        }
    }

    private fun updatePieChart(expenses: MutableList<Expense>) {
        if (expenses.filter { it.cost < 0 } .sumBy { it.cost } != 0) {
            expenseEmptyMsg.visibility = View.GONE
            expenseChart.visibility = View.VISIBLE
            expenseChart.createPieData(expenses)
        } else {
            expenseChart.visibility = View.GONE
            expenseEmptyMsg.visibility = View.VISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateRunningCharges(expenses: MutableList<Expense>) {
        val expensesOnly = expenses.filter { it.cost < 0 }
        val runningCharges = expensesOnly.filter { it.interval != NON_RECURRING }
        val variableExpenses = expensesOnly.filter { it.interval == NON_RECURRING }

        val totalAmount = expensesOnly.sumBy {it.cost } .absoluteValue
        val runningTotalAmount = runningCharges.sumBy { it.cost } .absoluteValue
        val variableTotalAmount = variableExpenses.sumBy { it.cost } .absoluteValue

        val runningChargePercentage = if (totalAmount != 0) {
            runningTotalAmount.toFloat()/totalAmount.toFloat()
        } else 0f


        val variableExpensePercentage = if (totalAmount != 0) {
            1 - runningChargePercentage
        } else 0f

        val formatter = NumberFormat.getPercentInstance().apply { maximumFractionDigits = 1 }

        runningChargeText.text = SimpleBudgetApp.createCurrencyString(runningTotalAmount)
        variableExpenseText.text = SimpleBudgetApp.createCurrencyString(variableTotalAmount)

        runningChargePercentText.text = "(${formatter.format(runningChargePercentage)})"
        variableExpensePercentageText.text = "(${formatter.format(variableExpensePercentage)})"

    }
}