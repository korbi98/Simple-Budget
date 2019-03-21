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

package com.korbi.simplebudget

import android.os.Bundle
import android.view.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import com.jakewharton.threetenabp.AndroidThreeTen
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.DateHelper
import com.korbi.simplebudget.ui.SELECT_ALL
import com.korbi.simplebudget.ui.TYPE_BOTH
import com.korbi.simplebudget.ui.dialogs.SetupDialog
import com.korbi.simplebudget.ui.fragments.*
import kotlinx.android.synthetic.main.activity_main.*
import org.threeten.bp.LocalDate

class MainActivity : AppCompatActivity() {

    val expandedStateMap = HashMap<Int, Boolean>()

    var typeSelection = TYPE_BOTH //0 for both, 1 for expenses, 2 for income
    var dateSelection = SELECT_ALL //0 last 30 days, 1 last 90 days, 2 this year, 3 all time
    lateinit var fromDateSelection: LocalDate
    lateinit var toDateSelection: LocalDate
    lateinit var categorySelection: BooleanArray //1 if category selected 0 else

    private val mOnNavigationItemSelectedListener =
                                BottomNavigationView.OnNavigationItemSelectedListener { item ->

        when (item.itemId) {
            R.id.navigation_history -> {
                supportActionBar?.title = getString(R.string.title_history)
                showFragment(HistoryFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                supportActionBar?.title = getString(R.string.title_dashboard)
                showFragment(DashboardFragment())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_statistic -> {
                supportActionBar?.title = getString(R.string.title_statistic)
                showFragment(StatisticFragment())
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        DBhandler.createInstance(this, resources.getStringArray(R.array.default_categories))
        DateHelper.createInstance()
        AndroidThreeTen.init(this)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        navigation.selectedItemId = R.id.navigation_dashboard

        // make sure, that the keyboard doesn't push the bottomnavigationbar upwards
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        categorySelection = BooleanArray(DBhandler.getInstance().getAllCategories().size) { true }
        fromDateSelection = LocalDate.now()
        toDateSelection = LocalDate.now()

        if (SimpleBudgetApp.pref.getBoolean(
                        getString(R.string.settings_key_initial_start), true)) {
            SetupDialog().show(supportFragmentManager, "setup_dialog")
        }

        showFragment(DashboardFragment())
    }

    override fun onPause() {
        super.onPause()
        updateWidget()
    }

    fun showFragment(fragment: androidx.fragment.app.Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

    private fun updateWidget() {
        sendBroadcast(SimpleBudgetApp.updateWidgetIntent(this, application))
    }
}
