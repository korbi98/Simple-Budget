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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.github.mikephil.charting.charts.PieChart
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.ui.DistributionPieChart
import kotlinx.android.synthetic.main.fragment_distribution.view.*


class DistributionFragment : androidx.fragment.app.Fragment(),
        StatisticFragment.DateSelectionListener {

    private lateinit var expenseChart: DistributionPieChart
    private lateinit var incomeChart: DistributionPieChart
    private lateinit var expenseEmptyMsg: TextView
    private lateinit var incomeEmptyMsg: TextView



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {


        val rootView = inflater.inflate(R.layout.fragment_distribution, container, false)

        expenseChart = rootView.distribution_expense_chart
        incomeChart = rootView.distribution_income_chart
        expenseEmptyMsg = rootView.distribution_expense_empty_msg
        incomeEmptyMsg = rootView.distribution_income_empty_msg

        return rootView
    }

    override fun onResume() {
        super.onResume()
        updateView()
    }

    override fun onDateSelectionChange() {
        updateView()
    }

    private fun updateView() {
        val dashboard = requireParentFragment() as StatisticFragment
        with(dashboard) {
            val expenses = if (getInterval() != -1) {
                getExpensesForInterval(getIntervalType(), getInterval())
            } else {
                getExpensesForInterval(SimpleBudgetApp.pref.getInt(
                        getString(R.string.dashboard_time_selection_key), 1), 0)
            }

            if (expenses.filter { it.cost < 0 } .sumBy { it.cost } != 0) {
                expenseEmptyMsg.visibility = View.GONE
                expenseChart.visibility = View.VISIBLE
                expenseChart.createPieData(expenses)
            } else {
                expenseChart.visibility = View.GONE
                expenseEmptyMsg.visibility = View.VISIBLE
            }

            if (expenses.filter { it.cost > 0 } .sumBy { it.cost } != 0) {
                incomeEmptyMsg.visibility = View.GONE
                incomeChart.visibility = View.VISIBLE
                incomeChart.createPieData(expenses, true)
            } else {
                incomeChart.visibility = View.GONE
                incomeEmptyMsg.visibility = View.VISIBLE
            }
        }
    }
}