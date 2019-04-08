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
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.korbi.simplebudget.MainActivity
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.DateHelper
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.ui.AddExpenses
import com.korbi.simplebudget.ui.IncomeManager
import com.korbi.simplebudget.ui.ManageCategories
import com.korbi.simplebudget.ui.SettingsActivity
import kotlinx.android.synthetic.main.fragment_dashboard.view.*
import kotlinx.android.synthetic.main.interval_backdrop.view.*
import org.threeten.bp.LocalDate
import org.threeten.bp.Year
import org.threeten.bp.YearMonth


const val ALL_TIME = 4
const val YEARLY_INTERVAL = 3
const val QUARTERLY_INTERVAL = 2
const val MONTHLY_INTERVAL = 1
const val WEEKLY_INTERVAL = 0

class DashboardFragment : androidx.fragment.app.Fragment(), MainActivity.OnBackListener {

    private lateinit var listener: DateSelectionListener
    private lateinit var timeSelectionSpinner:Spinner
    private lateinit var dateRangeSelectionLayout: View
    private lateinit var expensesTextView: TextView
    private lateinit var incomeTextView: TextView
    private lateinit var balanceTextView: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var intervalLayout: LinearLayout
    private lateinit var mainLayout: RelativeLayout
    private lateinit var intervalChipGroup: ChipGroup
    private lateinit var mOptionsMenu: Menu
    private lateinit var intervalTextView: TextView
    private var deltaY = 0f
    private var offset = 0f
    private val registeredFragments = SparseArray<Fragment>()

    private val db = DBhandler.getInstance()

    interface DateSelectionListener {
        fun onDateSelectionChange()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_dashboard, container, false).apply {

            fab.setOnClickListener {
                val addExpenseActivity = Intent(context, AddExpenses::class.java)
                startActivity(addExpenseActivity)
            }

            intervalTextView = dashboard_interval_text_view
            timeSelectionSpinner = dashboard_time_selection_spinner
            dateRangeSelectionLayout = dashboard_time_selection_layout
            expensesTextView = dashboard_total_expenses
            incomeTextView = dashboard_total_income
            balanceTextView = dashboard_balance
            intervalLayout = dashboard_interval_layout
            mainLayout =dashboard_main_layout
            intervalChipGroup = dashboard_interval_chip_group.apply {
                setOnCheckedChangeListener { _, _ ->
                    setupTimeSelectionSpinner(getIntervalType())
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
                    deltaY = intervalLayout.height.toFloat()
                    offset = intervalTextView.height.toFloat()
                    intervalLayout.visibility = View.GONE
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
                    sumExpenses(getExpensesForInterval(getIntervalType(), position))
                    listener.onDateSelectionChange()
                }
            }

            selectIntervalChip()
            setHasOptionsMenu(true)
        }
    }

    override fun onResume() {
        super.onResume()
        updateIntervalText()
        SimpleBudgetApp.handleRecurringEntries()
        setupTimeSelectionSpinner(getIntervalType())
        sumExpenses(getExpensesForInterval(getIntervalType(),
                timeSelectionSpinner.selectedItemPosition))
        if (::listener.isInitialized) listener.onDateSelectionChange()
    }

    override fun onBackPressed() {
        if (intervalLayout.isVisible) {
            intervalLayout.visibility = View.GONE
            updateIntervalText()
        } else {
            activity?.finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_dashboard, menu)
        mOptionsMenu = menu
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

            val intervalMenu = findItem(R.id.menu_dashboard_time_interval)
            val categoryMenu = findItem(R.id.menu_dashboard_categories)
            val incomeMenu = findItem(R.id.menu_dashboard_regular_income)
            val settingsMenu = findItem(R.id.menu_dashboard_settings)
            val intervalDoneMenu = findItem(R.id.menu_dashboard_interval_done)

            intervalDoneMenu.isVisible = intervalLayout.isVisible
            intervalMenu.isVisible = !intervalLayout.isVisible
            categoryMenu.isVisible = !intervalLayout.isVisible
            incomeMenu.isVisible = !intervalLayout.isVisible
            settingsMenu.isVisible = !intervalLayout.isVisible
        }
    }

    private fun setupTimeSelectionSpinner(intervalType: Int) {

        dateRangeSelectionLayout.visibility = View.VISIBLE

        val optionsArray = when (intervalType) {
            WEEKLY_INTERVAL -> DateHelper.getWeekSpinnerArray()
            QUARTERLY_INTERVAL -> DateHelper.getQuarterSpinnerArray()
            YEARLY_INTERVAL -> DateHelper.getYearSpinnerArray()
            ALL_TIME -> {
                dateRangeSelectionLayout.visibility = View.GONE
                sumExpenses(getExpensesForInterval(ALL_TIME, 0))

                if (::listener.isInitialized) {
                    listener.onDateSelectionChange()
                }

                Array(0){""}
            }
            else -> DateHelper.getMonthSpinnerArray()
        }
        val position = when (intervalType) {
            WEEKLY_INTERVAL -> optionsArray.indexOf(getString(R.string.this_week))
            MONTHLY_INTERVAL -> optionsArray.indexOf(getString(R.string.this_month))
            QUARTERLY_INTERVAL -> optionsArray.indexOf(getString(R.string.this_quarter))
            YEARLY_INTERVAL -> optionsArray.indexOf(Year.now().toString())
            else -> 0
        }

        timeSelectionSpinner.adapter = ArrayAdapter<String>(requireContext(),
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
                var weeks = DateHelper.getWeeks()
                if (weeks.size > 0) {
                    weeks = weeks.subList(1, weeks.size) .filter {
                        !db.getExpensesByDate(it[0], it[1]).isEmpty() ||
                                DateHelper.isBetween(LocalDate.now(), it[0], it[1])
                    } .toMutableList()
                }
                weeks.add(0, DateHelper.getWeeks()[0])

                val week = weeks[selectedInterval]
                startDate = week[0]
                endDate = week[1]
            }
            MONTHLY_INTERVAL -> {
                var months = DateHelper.getMonths()
                if (months.size > 0) {
                    months = months.subList(1, months.size).filter {
                        !db.getExpensesByDate(it.atDay(1), it.atEndOfMonth()).isEmpty()
                                || DateHelper.isBetween(LocalDate.now(),
                                it.atDay(1), it.atEndOfMonth())
                    } .toMutableList()
                }
                months.add(0, DateHelper.getMonths()[0])

                val month = months[selectedInterval]
                startDate = month.atDay(1)
                endDate = month.atEndOfMonth()
            }
            QUARTERLY_INTERVAL -> {
                val quarter = DateHelper.getQuarters()[selectedInterval]
                startDate = LocalDate.of(quarter[1], 1, 1)
                        .plusMonths(3*(quarter[0] - 1).toLong())
                endDate = YearMonth.of(quarter[1], 3).atEndOfMonth()
                        .plusMonths(3*(quarter[0] - 1).toLong())
            }
            YEARLY_INTERVAL -> {
                startDate = LocalDate.of(DateHelper.getYears()[selectedInterval], 1, 1)
                endDate = LocalDate.of(DateHelper.getYears()[selectedInterval], 12, 31)
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

    fun getInterval(): Int {
        return  timeSelectionSpinner.selectedItemPosition
    }

    private fun selectIntervalChip() {
        val selection = SimpleBudgetApp.pref.getInt(
                getString(R.string.dashboard_time_selection_key), MONTHLY_INTERVAL)

        intervalChipGroup.check( when (selection) {
            WEEKLY_INTERVAL -> R.id.chip_weekly
            QUARTERLY_INTERVAL -> R.id.chip_quarterly
            YEARLY_INTERVAL -> R.id.chip_yearly
            ALL_TIME -> R.id.chip_all_time
            else -> R.id.chip_monthly
        })
    }

    fun getIntervalType(): Int {
        val default = SimpleBudgetApp.pref.getInt(
                getString(R.string.dashboard_time_selection_key), MONTHLY_INTERVAL)

        return when (intervalChipGroup.checkedChipId) {
            R.id.chip_weekly -> WEEKLY_INTERVAL
            R.id.chip_monthly -> MONTHLY_INTERVAL
            R.id.chip_quarterly -> QUARTERLY_INTERVAL
            R.id.chip_yearly -> YEARLY_INTERVAL
            R.id.chip_all_time -> ALL_TIME
            else -> default
        } .also {
            with(SimpleBudgetApp.pref.edit()) {
                putInt(getString(R.string.dashboard_time_selection_key), it)
                apply()
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        when (hidden) {
            true -> {
                if (intervalLayout.isVisible) {
                    intervalLayout.visibility = View.GONE
                    intervalTextView.visibility = View.VISIBLE
                    updateOptionsMenu()
                }
            }
            false -> {
                updateIntervalText()
            }
        }
    }

    private fun updateIntervalText() {
        val intervalString = getString(R.string.dashboard_time_selection) + " " +
                when (timeSelectionSpinner.selectedItem) {
                    null -> getString(R.string.all_time).toLowerCase()
                    else -> timeSelectionSpinner.selectedItem.toString().toLowerCase()
                }
        intervalTextView.text = intervalString
        (activity as MainActivity).setTitle(when (timeSelectionSpinner.selectedItem) {
            null -> getString(R.string.all_time)
            else -> timeSelectionSpinner.selectedItem.toString()
        })
    }

    private fun hideIntervalLayout() {

        deltaY = intervalLayout.height.toFloat()

        intervalLayout.animation = TranslateAnimation(0f,0f, 0f, -deltaY).apply {
            duration = 200
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    intervalLayout.visibility = View.GONE
                    updateOptionsMenu()
                    updateIntervalText()
                }
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    (activity as AppCompatActivity).supportActionBar?.elevation = 4f
                }
            })
        }

        mainLayout.animation = TranslateAnimation(0f,0f, deltaY - offset, 0f).apply {
            duration = 200
        }

        intervalTextView.visibility = View.VISIBLE
        intervalTextView.animation = AlphaAnimation(0f, 1f).apply {
            duration = 400
        }

        intervalTextView.invalidate()

    }

    private fun showIntervalLayout() {

        (activity as AppCompatActivity).supportActionBar?.elevation = 0f

        // for some reason, animation does not trigger when intervalTextView is not gone
        intervalTextView.visibility = View.GONE
        intervalTextView.visibility = View.VISIBLE

        intervalLayout.animation = TranslateAnimation(0f,0f, -deltaY, 0f).apply {
            duration = 200
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    intervalLayout.visibility = View.VISIBLE
                    updateOptionsMenu()
                }
            })
        }

        mainLayout.animation = TranslateAnimation(0f,0f, 0f, deltaY - offset).apply {
            duration = 200
        }

        intervalTextView.animation = AlphaAnimation(1f, 0f).apply {
            duration = 200
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {
                    intervalTextView.visibility = View.GONE
                }
            })
        }

        (activity as MainActivity).setTitle(getString(R.string.select_interval))

        selectIntervalChip()
    }
}
