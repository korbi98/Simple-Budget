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

import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.tabs.TabLayout
import com.korbi.simplebudget.MainActivity
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.logic.ALL_TIME
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.logic.IntervalSelectionBackdropHelper
import com.korbi.simplebudget.ui.AddExpenses
import com.korbi.simplebudget.ui.IncomeManager
import com.korbi.simplebudget.ui.ManageCategories
import com.korbi.simplebudget.ui.SettingsActivity
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import kotlinx.android.synthetic.main.interval_backdrop.view.*
import com.korbi.simplebudget.logic.MenuAnimator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_dashboard.*

class DashboardFragment : androidx.fragment.app.Fragment(), MainActivity.OnBackListener {

    private lateinit var listener: DateSelectionListener
    private lateinit var timeSelectionSpinner:Spinner
    private lateinit var intervalSelectionLayout: View
    private lateinit var expensesTextView: TextView
    private lateinit var incomeTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var backdropLayout: LinearLayout
    private lateinit var mOptionsMenu: Menu
    lateinit var intervalHelper: IntervalSelectionBackdropHelper
    private val registeredFragments = SparseArray<Fragment>()

    interface DateSelectionListener {
        fun onDateSelectionChange()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_dashboard, container, false).apply {

            timeSelectionSpinner = backdrop_time_selection_spinner
            intervalSelectionLayout = backdrop_time_selection_layout
            expensesTextView = dashboard_total_expenses
            incomeTextView = dashboard_total_income
            balanceTextView = dashboard_balance
            backdropLayout = dashboard_interval_layout

            intervalHelper = IntervalSelectionBackdropHelper(
                    context = context,
                    backdropLayout = backdropLayout,
                    intervalChipGroup = backdrop_interval_chip_group,
                    intervalSpinner = timeSelectionSpinner,
                    intervalLayout = intervalSelectionLayout,
                    mainLayout = dashboard_main_layout).apply {
                onAllTimeSelected = {
                    sumExpenses(getExpensesForInterval(ALL_TIME, 0))

                    if (::listener.isInitialized) {
                        listener.onDateSelectionChange()
                    }
                }
            }

            // prevent chips from being unselected
            chip_weekly.setOnClickListener { chip_weekly.isChecked = true }
            chip_monthly.setOnClickListener { chip_monthly.isChecked = true }
            chip_quarterly.setOnClickListener { chip_quarterly.isChecked = true }
            chip_yearly.setOnClickListener { chip_yearly.isChecked = true }
            chip_all_time.setOnClickListener { chip_all_time.isChecked = true }

            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (viewTreeObserver.isAlive)
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                    intervalHelper.deltaY = backdropLayout.height.toFloat()
                    backdropLayout.visibility = View.GONE
                    if (::mOptionsMenu.isInitialized) updateOptionsMenu()
                }
            })


            val viewPager = dashboard_viewpager
            viewPager.adapter = object : FragmentStatePagerAdapter(childFragmentManager) {

                override fun getCount(): Int {
                    return 2
                }

                override fun instantiateItem(container: ViewGroup, position: Int): Any {
                    val fragment = super.instantiateItem(container, position) as Fragment
                    registeredFragments.put(position, fragment)
                    return fragment
                }

                override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                    registeredFragments.remove(position)
                    super.destroyItem(container, position, `object`)
                }

                override fun getItem(position: Int): Fragment {
                    return when (position) {
                        1 -> PieChartFragment()
                        else -> {
                            val fragment = BudgetFragment()
                            setListener(fragment.getListener())
                            fragment
                        }
                    }
                }
            }

            tabLayout = dashboard_tabs.apply {
                addTab(this.newTab().setText(R.string.budget))
                addTab(this.newTab().setText(R.string.distribution))
                addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

                    override fun onTabSelected(tab: TabLayout.Tab) {
                        viewPager.currentItem = tab.position

                        when (viewPager.currentItem) {
                            1 -> {
                                val pie = registeredFragments[1] as PieChartFragment
                                setListener(pie.getListener())
                                pie.updateView()
                            }
                            else -> {
                                val budget = registeredFragments[viewPager.currentItem]
                                        as BudgetFragment
                                setListener(budget.getListener())
                                budget.updateView()
                            }
                        }
                    }
                    override fun onTabReselected(p0: TabLayout.Tab?) {}
                    override fun onTabUnselected(p0: TabLayout.Tab?) {}
                })
            }

            viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

            timeSelectionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {parent?.setSelection(0)}

                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int,
                                            id: Long) {
                    intervalHelper.run {
                        sumExpenses(getExpensesForInterval(getIntervalType(), position))
                    }
                    listener.onDateSelectionChange()
                }
            }

            intervalHelper.selectIntervalChip()
            setHasOptionsMenu(true)
        }
    }

    override fun onResume() {
        super.onResume()
        updateIntervalText()
        SimpleBudgetApp.handleRecurringEntries()
        intervalHelper.run {
            setupTimeSelectionSpinner(getIntervalType())
            sumExpenses(getExpensesForInterval(getIntervalType(), getInterval()))
        }
        if (::listener.isInitialized) listener.onDateSelectionChange()
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
            backdropLayout.visibility = View.GONE
            updateIntervalText()
        } else {
            activity?.finish()
        }
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
            hideIntervalLayout()
            true
        }
        R.id.menu_dashboard_time_interval -> {
            showIntervalLayout()
            true
        }
        R.id.menu_dashboard_categories -> {
            val categoryManager = Intent(context, ManageCategories::class.java)
            startActivity(categoryManager)
            true
        }
        R.id.menu_dashboard_regular_income -> {
            val incomeManager = Intent(context, IncomeManager::class.java)
            startActivity(incomeManager)
            true
        }
        R.id.menu_dashboard_settings -> {
            val settings = Intent(context, SettingsActivity::class.java)
            startActivity(settings)
            true
        }

        else -> super.onOptionsItemSelected(item)
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

    fun sumExpenses(expenses: MutableList<Expense>) {

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

    fun setListener(listener: DateSelectionListener) {
        this.listener = listener
    }

    private fun updateIntervalText() {
        (activity as MainActivity).setTitle(when (timeSelectionSpinner.selectedItem) {
            null -> getString(R.string.all_time)
            else -> timeSelectionSpinner.selectedItem.toString()
        })
    }

    private fun hideIntervalLayout() {

        intervalHelper.deltaY = backdropLayout.height.toFloat()

        // for some reason, animation does not trigger when first child of layout is not gone
        dashboard_table.visibility = View.GONE
        dashboard_table.visibility = View.VISIBLE

        intervalHelper.hideBackdrop {
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

    private fun showIntervalLayout() {

        (activity as AppCompatActivity).supportActionBar?.elevation = 0f

        // for some reason, animation does not trigger when first child of layout is not gone
        dashboard_main_layout.visibility = View.GONE
        dashboard_main_layout.visibility = View.VISIBLE

        dashboard_main_layout.invalidate()
        intervalHelper.showBackdrop()

        (activity as MainActivity).setTitle(getString(R.string.select_interval))

        intervalHelper.selectIntervalChip()

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
}
