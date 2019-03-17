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


class PieChartFragment : androidx.fragment.app.Fragment(),  DashboardFragment.DateSelectionListener {

    private lateinit var pieChart: PieChart

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_pie_chart, container, false)


        pieChart = rootView.findViewById(R.id.dashboard_pie_chart)
        val entries = arrayListOf(PieEntry(40f, 100), PieEntry(96f, 1))
        val dataset = PieData(PieDataSet(entries, "test"))
        pieChart.data = dataset

        return rootView
    }

    override fun onDateSelectionChange(expenses: MutableList<Expense>, intervalType: Int) {

    }

    fun getListener(): DashboardFragment.DateSelectionListener {
        return this
    }
}