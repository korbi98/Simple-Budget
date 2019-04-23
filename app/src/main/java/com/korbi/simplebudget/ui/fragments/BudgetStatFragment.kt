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
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.model.Expense
import com.korbi.simplebudget.ui.charts.BudgetSpiderChart
import com.korbi.simplebudget.ui.charts.TotalBudgetChart
import com.korbi.simplebudget.utilities.MONTHLY_INTERVAL
import com.korbi.simplebudget.utilities.WEEKLY_INTERVAL
import kotlinx.android.synthetic.main.fragment_budget_stat.view.*

class BudgetStatFragment : Fragment(), StatisticFragment.DateSelectionListener {

    private lateinit var spiderChart: BudgetSpiderChart
    private lateinit var chartEmptyMsg: TextView
    private lateinit var totalBudgetChart: TotalBudgetChart
    private lateinit var totalChartEmptyMsg: TextView
    private lateinit var totalBudgetChartTitle: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootview =  inflater.inflate(R.layout.fragment_budget_stat, container, false)

        spiderChart = rootview.stat_budget_spider_chart
        chartEmptyMsg = rootview.stat_budget_empty_msg
        totalBudgetChart = rootview.stat_total_budget_spider_chart
        totalChartEmptyMsg = rootview.stat_total_budget_empty_msg
        totalBudgetChartTitle = rootview.stat_total_budget_chart_title

        spiderChart.initChart()
        totalBudgetChart.initChart()
        totalBudgetChart.extraTopOffset = 4f

        return rootview
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

            updateSpiderChart(expenses, getIntervalType())
            updateTotalBudgetChart()
        }
    }

    private fun updateSpiderChart(expenses: MutableList<Expense>, interval: Int) {

        if (DBhandler.getInstance().getAllCategories().all { it.budget == 0 }) {
            chartEmptyMsg.visibility = View.VISIBLE
            spiderChart.visibility = View.GONE
        } else {
            chartEmptyMsg.visibility = View.GONE
            spiderChart.visibility = View.VISIBLE
            spiderChart.setChartData(expenses, interval)
        }
    }

    private fun updateTotalBudgetChart() {
        if (DBhandler.getInstance().getAllCategories().all { it.budget == 0 }) {
            totalChartEmptyMsg.visibility = View.VISIBLE
            totalBudgetChart.visibility = View.GONE
        } else {
            totalChartEmptyMsg.visibility = View.GONE
            totalBudgetChart.visibility = View.VISIBLE
            totalBudgetChart.setChartData()
        }

        val titleString = when(SimpleBudgetApp.pref.getString(
                requireContext().getString(R.string.settings_key_history_grouping), "1")) {
            WEEKLY_INTERVAL.toString() ->{
                requireContext().run {
                    getString(R.string.total_budget) + " " + getString(R.string.by_week)
                }
            }
            MONTHLY_INTERVAL.toString() -> {
                requireContext().run {
                    getString(R.string.total_budget) + " " + getString(R.string.by_month)
                }
            }
            else -> requireContext().getString(R.string.total_budget)
        }

        totalBudgetChartTitle.text = titleString
    }
}
