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

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.logic.Category
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.logic.adapters.BudgetAdapter
import com.korbi.simplebudget.ui.dialogs.BudgetDialog
import com.korbi.simplebudget.ui.dialogs.CAT_INDEX
import kotlinx.android.synthetic.main.fragment_budget.view.*

const val SET_TOTAL_BUDGET = -100

class BudgetFragment : androidx.fragment.app.Fragment(),
                        DashboardFragment.DateSelectionListener,
                        BudgetAdapter.OnLongItemClickListener {

    private lateinit var budgetRecycler: RecyclerView
    private lateinit var budgetAdapter: BudgetAdapter
    private lateinit var totalBudgetLayout: RelativeLayout
    private lateinit var totalBudgetTextView: TextView
    private lateinit var totalBudgetAmount: TextView
    private lateinit var totalBudgetProgress: ProgressBar
    private lateinit var emptyMessage: TextView
    private var totalBudget = 0
    private var totalBudgetInterval = MONTHLY_INTERVAL
    private var selectedInterval = MONTHLY_INTERVAL
    private var expenseList = mutableListOf<Expense>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootview = inflater.inflate(R.layout.fragment_budget, container, false)

        emptyMessage = rootview.budget_fragment_empty_message

        budgetAdapter = BudgetAdapter(expenseList, selectedInterval, this)

        budgetRecycler = rootview.dashboard_budget_recycler.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            adapter = budgetAdapter
        }

        totalBudgetTextView = rootview.budget_total_text
        totalBudgetAmount = rootview.budget_total_budget
        totalBudgetProgress = rootview.budget_total_progress
        totalBudgetLayout = rootview.budget_total_layout
        totalBudgetLayout.setOnLongClickListener {
            showBudgetDialog(SET_TOTAL_BUDGET)
            true
        }

        return rootview
    }

    override fun onResume() {
        super.onResume()
        updateView()
    }

    override fun onDateSelectionChange() {
        updateView()
    }

    override fun onLongClick(category: Category) {
        showBudgetDialog(category.id)
    }

    fun updateView() {

        val dashboard = requireParentFragment() as DashboardFragment
        expenseList = if (dashboard.getInterval() != -1) {
            dashboard.getExpensesForInterval(dashboard.getIntervalType(), dashboard.getInterval())
        } else {
            dashboard.getExpensesForInterval(SimpleBudgetApp.pref.getInt(
                    getString(R.string.dashboard_time_selection_key), 1), 0)
        }
        selectedInterval = when (dashboard.getInterval() != -1) {
            true -> dashboard.getIntervalType()
            false -> SimpleBudgetApp.pref.getInt(
                    getString(R.string.dashboard_time_selection_key), 1)
        }

        when (expenseList.none { it.cost < 0 }) {
            true -> {
                budgetRecycler.visibility = View.GONE
                emptyMessage.visibility = View.VISIBLE
            }
            false -> {
                budgetRecycler.visibility = View.VISIBLE
                emptyMessage.visibility = View.GONE
            }
        }

        totalBudget = SimpleBudgetApp.pref
                .getInt(getString(R.string.total_budget_key), 0)
        totalBudgetInterval = SimpleBudgetApp.pref
                .getInt(getString(R.string.total_budget_interval_key), MONTHLY_INTERVAL)

        totalBudgetAmount.text = getBudgetText()
        totalBudgetProgress.progress = getBudgetProgress()

        totalBudgetTextView.text = when (selectedInterval) {
            ALL_TIME -> getString(R.string.total_expenses)
            else -> getString(R.string.total_budget)
        }

        if (getBudgetProgress() > 100) {
            totalBudgetAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.expenseColor))
            totalBudgetProgress.progressTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.expenseColor))
        } else {
            totalBudgetAmount.setTextColor(ContextCompat.getColor(requireContext(),
                    R.color.text_color_white_secondary))
            totalBudgetProgress.progressTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        }

        budgetAdapter.setExpenses(expenseList)
        budgetAdapter.setInterval(selectedInterval)
        budgetAdapter.updateCategories()
    }

    fun getListener(): DashboardFragment.DateSelectionListener {
        return this
    }

    private fun showBudgetDialog(id: Int) {
        BudgetDialog().let {
            it.arguments = Bundle().apply { putInt(CAT_INDEX, id) }
            it.show(childFragmentManager, "budgetDialog")
        }
    }

    private fun getBudgetText(): String {

        val categoryExpenses = expenseList.filter { it.cost < 0}
        val categoryTotalSum = -categoryExpenses.sumBy { it.cost }
        val budget = getIntervalBudget(selectedInterval)

        val onLeft = SimpleBudgetApp.pref.getBoolean(
                SimpleBudgetApp.res.getString(R.string.settings_key_currency_left), false)

        var budgetString = SimpleBudgetApp.createCurrencyString(categoryTotalSum, true, !onLeft)

        budgetString = when {
            selectedInterval == ALL_TIME -> budgetString
            budget != 0 -> {
                val str = SimpleBudgetApp.createCurrencyString(budget, true, onLeft)
                "$budgetString / $str"
            }
            else -> getString(R.string.no_budget_set_info)
        }
        return budgetString
    }

    private fun getBudgetProgress(): Int {

        val categoryExpenses = expenseList.filter { it.cost < 0}
        val categoryTotalSum = -categoryExpenses.sumBy { it.cost }
        val budget = getIntervalBudget(selectedInterval)

        return when {
            selectedInterval == ALL_TIME -> 100
            categoryTotalSum > 0 && budget != 0 -> {
                ((categoryTotalSum.toFloat() / budget.toFloat())*100).toInt()
            }
            else -> 0
        }
    }

    private fun getIntervalBudget(interval: Int): Int {

        return when (interval) {
            ALL_TIME -> 0
            YEARLY_INTERVAL -> {
                when (totalBudgetInterval) {
                    WEEKLY_INTERVAL -> totalBudget * 52
                    else -> totalBudget * 12
                }
            }
            QUARTERLY_INTERVAL -> {
                when (totalBudgetInterval) {
                    WEEKLY_INTERVAL -> totalBudget * 13
                    else -> totalBudget * 4
                }
            }
            MONTHLY_INTERVAL -> {
                when (totalBudgetInterval) {
                    WEEKLY_INTERVAL -> totalBudget * 4
                    else -> totalBudget
                }
            }
            else -> {
                when (totalBudgetInterval) {
                    WEEKLY_INTERVAL -> totalBudget
                    else -> {
                        (totalBudget / 4.33f).toInt()
                    }
                }
            }
        }
    }
}