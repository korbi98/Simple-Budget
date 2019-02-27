package com.korbi.simplebudget

import android.content.Intent
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.ui.*
import com.korbi.simplebudget.ui.fragments.DashboardFragment
import com.korbi.simplebudget.ui.fragments.HistoryFragment
import com.korbi.simplebudget.ui.fragments.StatisticFragment
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val expandedStateMap = HashMap<Int, Boolean>()

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
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        navigation.selectedItemId = R.id.navigation_dashboard
        showFragment(DashboardFragment())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu_dashboard, menu)
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

    private fun showFragment(fragment: androidx.fragment.app.Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }
}
