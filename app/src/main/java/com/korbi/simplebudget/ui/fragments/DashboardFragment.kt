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

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import com.korbi.simplebudget.MainActivity
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.logic.*
import com.korbi.simplebudget.ui.IncomeManager
import com.korbi.simplebudget.ui.ManageCategories
import com.korbi.simplebudget.ui.SettingsActivity
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import kotlinx.android.synthetic.main.interval_backdrop.view.*
import com.korbi.simplebudget.logic.adapters.BudgetAdapter
import com.korbi.simplebudget.ui.dialogs.BudgetDialog
import com.korbi.simplebudget.ui.dialogs.CAT_INDEX
import com.korbi.simplebudget.ui.dialogs.SET_TOTAL_BUDGET
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_dashboard.*


class DashboardFragment : androidx.fragment.app.Fragment(),
                            MainActivity.OnBackListener,
                            BudgetAdapter.OnLongItemClickListener,
                            IntervalSelectionBackdropHelper {

    override lateinit var mContext: Context
    override lateinit var backdropLayout: LinearLayout
    override lateinit var intervalChipGroup: ChipGroup
    override lateinit var intervalSpinner: Spinner
    override lateinit var intervalSpinnerLayout: View
    override lateinit var mainLayout: View
    override var deltaY = 0f

    private lateinit var expensesTextView: TextView
    private lateinit var incomeTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var mOptionsMenu: Menu

    private lateinit var budgetRecycler: RecyclerView
    private lateinit var budgetAdapter: BudgetAdapter
    private lateinit var totalBudgetLayout: RelativeLayout
    private lateinit var totalBudgetTextView: TextView
    private lateinit var totalBudgetAmount: TextView
    private lateinit var totalBudgetProgress: ProgressBar
    private lateinit var emptyMessage: TextView
    private val budgetHelper = BudgetHelper()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_dashboard, container, false).apply {


            // prevent chips from being unselected
            chip_weekly.setOnClickListener { chip_weekly.isChecked = true }
            chip_monthly.setOnClickListener { chip_monthly.isChecked = true }
            chip_quarterly.setOnClickListener { chip_quarterly.isChecked = true }
            chip_yearly.setOnClickListener { chip_yearly.isChecked = true }
            chip_all_time.setOnClickListener { chip_all_time.isChecked = true }

            mContext = context
            backdropLayout = dashboard_backdrop_layout
            intervalChipGroup = backdrop_interval_chip_group
            intervalSpinner = backdrop_time_selection_spinner
            intervalSpinnerLayout = backdrop_time_selection_layout
            mainLayout = dashboard_main_layout

            expensesTextView = dashboard_total_expenses
            incomeTextView = dashboard_total_income
            balanceTextView = dashboard_balance

            // Setup Budget
            emptyMessage = budget_fragment_empty_message
            budgetAdapter = BudgetAdapter(this@DashboardFragment)

            budgetRecycler = dashboard_budget_recycler.apply {
                setHasFixedSize(true)
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                adapter = budgetAdapter
            }

            totalBudgetTextView = budget_total_text
            totalBudgetAmount = budget_total_budget
            totalBudgetProgress = budget_total_progress
            totalBudgetLayout = budget_total_layout
            totalBudgetLayout.setOnLongClickListener {
                showBudgetDialog(SET_TOTAL_BUDGET)
                true
            }

            // Measure height of interval backdrop
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (viewTreeObserver.isAlive)
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                    deltaY = backdropLayout.height.toFloat()
                    backdropLayout.visibility = View.GONE
                    if (::mOptionsMenu.isInitialized) updateOptionsMenu()
                }
            })

            initIntervalHelper()
            setHasOptionsMenu(true)
        }
    }

    override fun onResume() {
        super.onResume()
        updateIntervalText(false)
        SimpleBudgetApp.handleRecurringEntries()
        setupTimeSelectionSpinner(getIntervalType())
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        when (hidden) {
            true -> {
                if (backdropLayout.isVisible) {
                    backdropLayout.visibility = View.GONE
                    updateOptionsMenu()
                }
            }
            false -> {
                updateIntervalText()
            }
        }
    }

    override fun onBackPressed() {
        if (backdropLayout.isVisible) {
            hideBackdropLayout()
            updateIntervalText()
        } else {
            activity?.finish()
        }
    }

    override fun onIntervalSelected() {
        updateView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_dashboard, menu)
        mOptionsMenu = menu

        (activity as MainActivity).toolbar.overflowIcon?.let { MenuAnimator.showIcon(it) }

        updateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {

        R.id.menu_dashboard_interval_done -> {
            hideBackdropLayout()
            true
        }
        R.id.menu_dashboard_time_interval -> {
            showBackdropLayout()
            true
        }
        R.id.menu_dashboard_categories -> {
            val categoryManager = Intent(mContext, ManageCategories::class.java)
            startActivity(categoryManager)
            true
        }
        R.id.menu_dashboard_regular_income -> {
            val incomeManager = Intent(mContext, IncomeManager::class.java)
            startActivity(incomeManager)
            true
        }
        R.id.menu_dashboard_settings -> {
            val settings = Intent(mContext, SettingsActivity::class.java)
            startActivity(settings)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onLongClick(category: Category) {
        showBudgetDialog(category.id)
    }

    fun updateOptionsMenu() {
        with(mOptionsMenu) {

            findItem(R.id.menu_dashboard_time_interval).apply {
                isVisible = !backdropLayout.isVisible
                icon.alpha = 255
            }
            findItem(R.id.menu_dashboard_categories).isVisible = !backdropLayout.isVisible
            findItem(R.id.menu_dashboard_regular_income).isVisible = !backdropLayout.isVisible
            findItem(R.id.menu_dashboard_settings).isVisible = !backdropLayout.isVisible
            findItem(R.id.menu_dashboard_interval_done).isVisible = backdropLayout.isVisible
            (activity as MainActivity).toolbar.overflowIcon?.alpha = 255

        }
    }

    fun updateView() {

        sumExpenses()

        with(budgetHelper) {

            expenses = getExpensesForInterval()
            interval = getIntervalType()

            when (expenses.none { it.cost < 0 }) {
                true -> {
                    budgetRecycler.visibility = View.GONE
                    emptyMessage.visibility = View.VISIBLE
                }
                false -> {
                    budgetRecycler.visibility = View.VISIBLE
                    emptyMessage.visibility = View.GONE
                }
            }

            totalBudgetAmount.text = getTotalBudgetText()
            totalBudgetProgress.progress = getTotalBudgetProgress()

            totalBudgetTextView.text = when (getIntervalType()) {
                ALL_TIME -> getString(R.string.total_expenses)
                else -> getString(R.string.total_budget)
            }

            if (getTotalBudgetProgress() > 100) {
                totalBudgetAmount.setTextColor(ContextCompat.getColor(
                        requireContext(), R.color.expenseColor))
                totalBudgetProgress.progressTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.expenseColor))
            } else {
                totalBudgetAmount.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.text_color_white_secondary))
                totalBudgetProgress.progressTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.colorPrimary))
            }

            budgetAdapter.expenses = expenses
            budgetAdapter.interval = interval
            budgetAdapter.updateCategories()
        }
    }

    private fun sumExpenses() {

        val expenses = getExpensesForInterval()

        val totalExpenses = expenses.filter { it.cost < 0 } .sumBy { it.cost }
        val totalIncome = expenses.filter {it.cost > 0} .sumBy { it.cost }
        val balance = totalExpenses + totalIncome

        expensesTextView.text = SimpleBudgetApp.createCurrencyString(totalExpenses)
        incomeTextView.text = SimpleBudgetApp.createCurrencyString(totalIncome)
        balanceTextView.text = SimpleBudgetApp.createCurrencyString(balance)

        when  {
            balance < 0 -> balanceTextView.setTextColor(ContextCompat.getColor(requireContext(),
                                            R.color.expenseColor))
            balance > 0 -> balanceTextView.setTextColor(ContextCompat.getColor(requireContext(),
                                            R.color.incomeColor))
            else -> balanceTextView.setTextColor(ContextCompat.getColor(requireContext(),
                    R.color.neutralColor))
        }
    }

    private fun updateIntervalText(animate: Boolean = true) {
        if (animate) {
            (activity as MainActivity).setTitle(getIntervalString())
        } else {
            // don't animate title on startup
            (activity as MainActivity).title = getIntervalString()
        }
    }

    private fun hideBackdropLayout() {

        deltaY = backdropLayout.height.toFloat()

        hideBackdrop {
            updateIntervalText()
            (activity as AppCompatActivity).supportActionBar?.elevation = 4f
        }

        //dashboard_table.invalidate()

        with(mOptionsMenu) {

            MenuAnimator.setVisibility(findItem(R.id.menu_dashboard_interval_done), false) { // endAction
                MenuAnimator.setVisibility(findItem(R.id.menu_dashboard_time_interval), true)
                val overflowIcon = (activity as MainActivity).toolbar.overflowIcon
                findItem(R.id.menu_dashboard_categories).isVisible = true
                findItem(R.id.menu_dashboard_regular_income).isVisible = true
                findItem(R.id.menu_dashboard_settings).isVisible = true
                overflowIcon?.let { MenuAnimator.showIcon(it) }
            }
        }
    }

    private fun showBackdropLayout() {

        (activity as AppCompatActivity).supportActionBar?.elevation = 0f

        showBackdrop()

        (activity as MainActivity).setTitle(getString(R.string.select_interval))

        selectIntervalChip()

        with(mOptionsMenu) {

            val overflowIcon = (activity as MainActivity).toolbar.overflowIcon
            overflowIcon?.let {
                MenuAnimator.hideIcon(it) {
                    findItem(R.id.menu_dashboard_categories).isVisible = false
                    findItem(R.id.menu_dashboard_regular_income).isVisible = false
                    findItem(R.id.menu_dashboard_settings).isVisible = false
                }
            }

            MenuAnimator.setVisibility(findItem(R.id.menu_dashboard_time_interval), false) { // endAction
                MenuAnimator.setVisibility(findItem(R.id.menu_dashboard_interval_done), true)
            }
        }
    }

    private fun showBudgetDialog(id: Int) {
        BudgetDialog().let {
            it.arguments = Bundle().apply { putInt(CAT_INDEX, id) }
            it.show(childFragmentManager, "budgetDialog@Dashboard")
        }
    }
}
