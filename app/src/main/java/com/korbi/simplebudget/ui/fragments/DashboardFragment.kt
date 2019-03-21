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
import android.util.Log
import android.util.SparseArray
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.DateHelper
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.ui.AddExpenses
import com.korbi.simplebudget.ui.IncomeManager
import com.korbi.simplebudget.ui.ManageCategories
import com.korbi.simplebudget.ui.SettingsActivity
import org.threeten.bp.LocalDate
import org.threeten.bp.Year
import org.threeten.bp.YearMonth


const val ALL_TIME = 4
const val YEARLY_INTERVAL = 3
const val QUARTERLY_INTERVAL = 2
const val MONTHLY_INTERVAL = 1
const val WEEKLY_INTERVAL = 0

class DashboardFragment : androidx.fragment.app.Fragment() {

    private lateinit var listener: DateSelectionListener
    private lateinit var timeSelectionSpinner:Spinner
    private lateinit var actionBarSpinner: Spinner
    private lateinit var timeSelectionLayout: View
    private lateinit var expensesTextView: TextView
    private lateinit var incomeTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var tabLayout: TabLayout
    private val registeredFragments = SparseArray<Fragment>()

    private val db = DBhandler.getInstance()
    private val dh = DateHelper.getInstance()

    interface DateSelectionListener {
        fun onDateSelectionChange()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val fab:FloatingActionButton = rootView.findViewById(R.id.fab)
        fab.setOnClickListener {
            val addExpenseActivity = Intent(context, AddExpenses::class.java)
            startActivity(addExpenseActivity)
        }

        timeSelectionSpinner = rootView.findViewById(R.id.dashboard_time_selection_spinner)
        timeSelectionLayout = rootView.findViewById(R.id.dashboard_time_selection_layout)
        expensesTextView = rootView.findViewById(R.id.dashboard_total_expenses)
        incomeTextView = rootView.findViewById(R.id.dashboard_total_income)
        balanceTextView = rootView.findViewById(R.id.dashboard_balance)

        tabLayout = rootView.findViewById<TabLayout>(R.id.dashboard_tabs)
        tabLayout.addTab(tabLayout.newTab().setText(R.string.budget))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.distribution))

        val viewPager = rootView.findViewById<ViewPager>(R.id.dashboard_viewpager)
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

        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

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

        timeSelectionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {parent?.setSelection(0)}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int,
                                        id: Long) {
                sumExpenses(getExpensesForInterval(actionBarSpinner.selectedItemPosition, position))
                listener.onDateSelectionChange()
            }
        }

        setHasOptionsMenu(true)
        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_dashboard, menu)

        val spinnerItem = menu.findItem(R.id.menu_dashboard_time_interval)
        actionBarSpinner = spinnerItem.actionView as Spinner
        actionBarSpinner.adapter = ArrayAdapter.createFromResource(context!!,
                R.array.dashboard_time_interval, android.R.layout.simple_spinner_dropdown_item)

        actionBarSpinner.setSelection(
                SimpleBudgetApp.pref.getInt(
                        getString(R.string.dashboard_time_selection_key), MONTHLY_INTERVAL))

        actionBarSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?, position: Int, id: Long) {
                setupTimeSelectionSpinner(position)
                with(SimpleBudgetApp.pref.edit()) {
                    putInt(getString(R.string.dashboard_time_selection_key), position)
                    apply()
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        SimpleBudgetApp.handleRecurringEntries()
        if (this::actionBarSpinner.isInitialized) {
            setupTimeSelectionSpinner(actionBarSpinner.selectedItemPosition)
            sumExpenses(getExpensesForInterval(actionBarSpinner.selectedItemPosition,
                    timeSelectionSpinner.selectedItemPosition))
            listener.onDateSelectionChange()
        } else {
            if (!db.getExpensesByDate(db.getOldestDate(), db.getNewestDate()).isEmpty()) {
                sumExpenses(getExpensesForInterval(WEEKLY_INTERVAL, 0))
                if (::listener.isInitialized) {
                    listener.onDateSelectionChange()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {

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

            else -> false
        }
    }

    fun setupTimeSelectionSpinner(intervalType: Int) {

        timeSelectionLayout.visibility = View.VISIBLE
        val dh = DateHelper.getInstance()

        val optionsArray = when (intervalType) {
            WEEKLY_INTERVAL -> dh.getWeekSpinnerArray()
            QUARTERLY_INTERVAL -> dh.getQuarterSpinnerArray()
            YEARLY_INTERVAL -> dh.getYearSpinnerArray()
            ALL_TIME -> {
                timeSelectionLayout.visibility = View.GONE
                sumExpenses(getExpensesForInterval(ALL_TIME, 0))

                if (::listener.isInitialized) {
                    listener.onDateSelectionChange()
                }

                Array(0){""}
            }
            else -> dh.getMonthSpinnerArray()
        }
        val position = when (intervalType) {
            WEEKLY_INTERVAL -> optionsArray.indexOf(getString(R.string.this_week))
            MONTHLY_INTERVAL -> optionsArray.indexOf(getString(R.string.this_month))
            QUARTERLY_INTERVAL -> optionsArray.indexOf(getString(R.string.this_quarter))
            YEARLY_INTERVAL -> optionsArray.indexOf(Year.now().toString())
            else -> 0
        }

        timeSelectionSpinner.adapter = ArrayAdapter<String>(context!!,
                android.R.layout.simple_spinner_dropdown_item, optionsArray)

        if (position != -1) {
            timeSelectionSpinner.setSelection(position)
        }
    }

    fun getExpensesForInterval (intervalType: Int, selectedInterval: Int): MutableList<Expense> {
        var startDate = db.getOldestDate()
        var endDate = db.getNewestDate()

        when (intervalType) {
            WEEKLY_INTERVAL -> {
                var weeks = dh.getWeeks()
                if (weeks.size > 0) {
                    weeks = weeks.subList(1, weeks.size) .filter {
                        !db.getExpensesByDate(it[0], it[1]).isEmpty()
                    } .toMutableList()
                }
                Log.d("test", dh.getWeeks().size.toString())
                weeks.add(0, dh.getWeeks()[0])

                val week = weeks[selectedInterval]
                startDate = week[0]
                endDate = week[1]
            }
            MONTHLY_INTERVAL -> {
                var months = dh.getMonths()
                if (months.size > 0) {
                    months = months.subList(1, months.size).filter {
                        !db.getExpensesByDate(it.atDay(1), it.atEndOfMonth()).isEmpty()
                    } .toMutableList()
                }
                months.add(0, dh.getMonths()[0])

                val month = months[selectedInterval]
                startDate = month.atDay(1)
                endDate = month.atEndOfMonth()
            }
            QUARTERLY_INTERVAL -> {
                val quarter = dh.getQuarters()[selectedInterval]
                startDate = LocalDate.of(quarter[1], 1, 1)
                endDate = YearMonth.of(quarter[1], 3).atEndOfMonth()
                startDate = startDate.plusMonths(3*(quarter[0] - 1).toLong())
                endDate = endDate.plusMonths(3*(quarter[0] - 1).toLong())
            }
            YEARLY_INTERVAL -> {
                startDate = LocalDate.of(dh.getYears()[selectedInterval], 1, 1)
                endDate = LocalDate.of(dh.getYears()[selectedInterval], 12, 31)
            }
        }
        return db.getExpensesByDate(startDate, endDate)
    }

    fun sumExpenses(expenses: MutableList<Expense>) {

        val totalExpenses = expenses.filter { it.cost < 0 } .sumBy { it.cost }
        val totalIncome = expenses.filter {it.cost > 0} .sumBy { it.cost }
        val balance = totalExpenses + totalIncome

        expensesTextView.text = SimpleBudgetApp.createCurrencyString(totalExpenses)
        incomeTextView.text = SimpleBudgetApp.createCurrencyString(totalIncome)
        balanceTextView.text = SimpleBudgetApp.createCurrencyString(balance)

        when  {
            balance < 0 -> balanceTextView.setTextColor(ContextCompat.getColor(context!!,
                                            R.color.expenseColor))
            balance > 0 -> balanceTextView.setTextColor(ContextCompat.getColor(context!!,
                                            R.color.incomeColor))
            else -> balanceTextView.setTextColor(ContextCompat.getColor(context!!,
                    R.color.neutralColor))
        }
    }

    fun setListener(listener: DateSelectionListener) {
        this.listener = listener
    }

    fun getIntervalType(): Int {
        return actionBarSpinner.selectedItemPosition
    }

    fun getInterval(): Int {
        return  timeSelectionSpinner.selectedItemPosition
    }

    fun actionBarSinnerInitialized(): Boolean {
        return ::actionBarSpinner.isInitialized
    }
}
