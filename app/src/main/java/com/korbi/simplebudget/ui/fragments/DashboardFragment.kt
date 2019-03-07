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
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout

import com.korbi.simplebudget.R
import com.korbi.simplebudget.logic.DateHelper
import com.korbi.simplebudget.ui.AddExpenses
import com.korbi.simplebudget.ui.IncomeManager
import com.korbi.simplebudget.ui.ManageCategories
import com.korbi.simplebudget.ui.SettingsActivity

class DashboardFragment : androidx.fragment.app.Fragment() {

    private lateinit var timeSelectionSpinner:Spinner
    private lateinit var actionBarSpinner: Spinner
    private lateinit var timeSelectionLayout: View
    private lateinit var firstDivider: View

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
        actionBarSpinner = spinnerItem.actionView as Spinner
        actionBarSpinner.adapter = ArrayAdapter.createFromResource(context!!,
                R.array.dashboard_time_interval, android.R.layout.simple_spinner_dropdown_item)

        actionBarSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?, position: Int, id: Long) {
                setupTimeSelectionSpinner(position)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (this::actionBarSpinner.isInitialized) {
            setupTimeSelectionSpinner(actionBarSpinner.selectedItemPosition)
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
        val dh = DateHelper.getInstance()

        val optionsArray = when (option) {
            0 -> dh.getWeekSpinnerArray()
            2 -> dh.getQuarterSpinnerArray()
            3 -> dh.getYearSpinnerArray()
            4 -> {
                firstDivider.visibility = View.GONE
                timeSelectionLayout.visibility = View.GONE
                Array(0){""}
            }

            else -> dh.getMonthSpinnerArray()
        }

        timeSelectionSpinner.adapter = ArrayAdapter<String>(context!!,
                android.R.layout.simple_spinner_dropdown_item, optionsArray)
    }

}
