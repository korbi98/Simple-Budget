package com.korbi.simplebudget.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.view.MenuItemCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

import com.korbi.simplebudget.R
import com.korbi.simplebudget.ui.AddExpenses
import com.korbi.simplebudget.ui.IncomeManager
import com.korbi.simplebudget.ui.ManageCategories
import com.korbi.simplebudget.ui.SettingsActivity

class DashboardFragment : androidx.fragment.app.Fragment() {

    lateinit var timeSelectionSpinner:Spinner
    lateinit var timeSelectionLayout: View
    lateinit var firstDivider: View

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
        firstDivider = rootView.findViewById(R.id.dashboard_first_divider)

        val tabLayout = rootView.findViewById<TabLayout>(R.id.dashboard_tabs)
        tabLayout.addTab(tabLayout.newTab().setText(R.string.budget))
        tabLayout.addTab(tabLayout.newTab().setText(R.string.pi_chart))

        val viewPager = rootView.findViewById<ViewPager>(R.id.dashboard_viewpager)
        viewPager.adapter = object : FragmentStatePagerAdapter(fragmentManager!!) {

            override fun getCount(): Int {
                return 2
            }

            override fun getItem(position: Int): Fragment {
                return when (position) {
                    1 -> PiChartFragment()
                    else -> BudgetFragment()
                }
            }

        }

        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
            }

            override fun onTabReselected(p0: TabLayout.Tab?) {}
            override fun onTabUnselected(p0: TabLayout.Tab?) {}

        })

        setHasOptionsMenu(true)
        return rootView
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_dashboard, menu)

        val spinnerItem = menu.findItem(R.id.menu_dashboard_time_interval)
        val spinner: Spinner = spinnerItem.actionView as Spinner
        spinner.adapter = ArrayAdapter.createFromResource(context!!,R.array.dashboard_time_interval,
                android.R.layout.simple_spinner_dropdown_item)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                setupTimeSelectionSpinner(position)
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

    fun setupTimeSelectionSpinner(option: Int) {

        firstDivider.visibility = View.VISIBLE
        timeSelectionLayout.visibility = View.VISIBLE

        val optionsArray = when (option) {
            0 -> resources.getStringArray(R.array.dashboard_time_selection_weekly)
            2 -> resources.getStringArray(R.array.dashboard_time_selection_quarterly)
            3 -> resources.getStringArray(R.array.dashboard_time_selection_yearly)
            4 -> {
                firstDivider.visibility = View.GONE
                timeSelectionLayout.visibility = View.GONE
                Array<String>(0){""}
            }

            else -> resources.getStringArray(R.array.dashboard_time_selection_monthly)
        }

        timeSelectionSpinner.adapter = ArrayAdapter<String>(context!!,
                android.R.layout.simple_spinner_dropdown_item, optionsArray)
    }

}
