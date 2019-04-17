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

package com.korbi.simplebudget.ui

import android.content.Intent
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.ui.dialogs.SetupDialog
import com.korbi.simplebudget.ui.fragments.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.ClassCastException

class MainActivity : AppCompatActivity() {

    interface OnBackListener {
        fun onBackPressed()
    }

    private lateinit var listener: OnBackListener

    var dashboard: DashboardFragment? = null
    private var history: HistoryFragment? = null
    private var statistics: StatisticFragment? = null
    private var activeFragment: Fragment? = dashboard

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->

        if (item.itemId != navigation.selectedItemId) {
            when (item.itemId) {
                R.id.navigation_history -> {
                    setTitle(getString(R.string.title_history))
                    showFragment(history)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_dashboard -> {
                    showFragment(dashboard)
                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_statistic -> {
                    setTitle(title = getString(R.string.title_statistic))
                    showFragment(statistics)
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        } else false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        TransitionManager.beginDelayedTransition(main_layout)

        setSupportActionBar(toolbar)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        navigation.selectedItemId = R.id.navigation_dashboard


        showFragment(dashboard)

        // make sure, that the keyboard doesn't push the bottomnavigationbar upwards
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        if (SimpleBudgetApp.pref.getBoolean(
                        getString(R.string.settings_key_initial_start), true)) {
            SetupDialog().show(supportFragmentManager, "setup_dialog")
        }

        fab.setOnClickListener {
            val addExpenseActivity = Intent(this, AddExpenses::class.java)
            startActivity(addExpenseActivity)
        }
    }

    override fun onPause() {
        super.onPause()
        updateWidget()
    }

    override fun onBackPressed() {
        if (dashboard?.isHidden != false) {
            showFragment(dashboard)
            navigation.selectedItemId = R.id.navigation_dashboard
        } else {
            if (::listener.isInitialized) listener.onBackPressed()
        }
    }

    fun showFragment(fragment: Fragment?) {

        with(supportFragmentManager.beginTransaction()) {

            var newFragment = fragment

            if (fragment == null) {
                when {
                    fragment === history -> {
                        history = HistoryFragment()
                        newFragment = history
                    }
                    fragment === dashboard -> {
                        dashboard = DashboardFragment()
                        newFragment = dashboard
                        dashboard?.let{ listener = it }
                    }
                    fragment === statistics -> {
                        statistics = StatisticFragment()
                        newFragment = statistics
                    }
                }
                newFragment?.let { add(R.id.fragment_container, it) }
            }

            activeFragment?.let { hide(it) }
            newFragment?.let { show(it) }
            activeFragment = newFragment

            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            commit()
        }
    }

    fun setTitle(title: String) {

        val titleView: TextView? = try{
            toolbar.getChildAt(0) as TextView?
        } catch (e: ClassCastException) {
            supportActionBar?.title = title
            null
        }

        val anim = AlphaAnimation(1f, 0f).apply {
            duration = 150
            repeatCount = 1
            repeatMode = Animation.REVERSE
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {
                    titleView?.text = title
                }
                override fun onAnimationEnd(animation: Animation?) {}
            })
        }
        titleView?.startAnimation(anim)
    }

    fun animateLayoutChanges(duration: Long = 100) {
        TransitionManager.beginDelayedTransition(main_layout, AutoTransition().also {
            it.duration = duration
        })
    }

    private fun updateWidget() {
        sendBroadcast(SimpleBudgetApp.updateWidgetIntent(this, application))
    }
}
