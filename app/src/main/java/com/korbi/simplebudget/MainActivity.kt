package com.korbi.simplebudget

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.korbi.simplebudget.ui.AddExpenses
import com.korbi.simplebudget.ui.DashboardFragment
import com.korbi.simplebudget.ui.HistoryFragment
import com.korbi.simplebudget.ui.SettingsActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener =
                                BottomNavigationView.OnNavigationItemSelectedListener { item ->

        val fragment: Fragment

        when (item.itemId) {
            R.id.navigation_history -> {
                supportActionBar?.setTitle(R.string.title_history)
                fragment = HistoryFragment()
                showFragment(fragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                supportActionBar?.setTitle(R.string.title_dashboard)
                fragment = DashboardFragment()
                showFragment(fragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_statistic -> {
                supportActionBar?.setTitle(R.string.title_statistic)
                fragment = HistoryFragment()
                showFragment(fragment)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private fun showFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        showFragment(DashboardFragment())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        return when (item?.itemId) {

            R.id.menu_categories -> {
                true
            }

            R.id.menu_regular_income -> {

                true
            }

            R.id.menu_settings -> {
                val settings = Intent(this, SettingsActivity::class.java)
                startActivity(settings)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

}
