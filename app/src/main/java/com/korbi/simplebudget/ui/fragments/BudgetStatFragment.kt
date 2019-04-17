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

import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.ui.BudgetSpiderChart
import kotlinx.android.synthetic.main.fragment_budget_stat.view.*

class BudgetStatFragment : Fragment(), StatisticFragment.DateSelectionListener {

    private lateinit var spiderChart: BudgetSpiderChart

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootview =  inflater.inflate(R.layout.fragment_budget_stat, container, false)

        spiderChart = rootview.stat_budget_spider_chart

        return rootview
    }

    override fun onDateSelectionChange() {

    }

    fun updateView() {
        val dashboard = requireParentFragment() as StatisticFragment
        with(dashboard) {
            val expenses = if (getInterval() != -1) {
                getExpensesForInterval(getIntervalType(), getInterval())
            } else {
                getExpensesForInterval(SimpleBudgetApp.pref.getInt(
                        getString(R.string.dashboard_time_selection_key), 1), 0)
            }

            spiderChart.setChartData(expenses, getIntervalType())

        }
    }

}
