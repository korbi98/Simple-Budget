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
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.SparseArray
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.korbi.simplebudget.MainActivity
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.logic.IntervalSelectionBackdropHelper
import com.korbi.simplebudget.logic.MenuAnimator
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.interval_backdrop.view.*
import kotlinx.android.synthetic.main.fragment_statistic.view.*


class StatisticFragment : androidx.fragment.app.Fragment(), IntervalSelectionBackdropHelper {

    override lateinit var mContext: Context
    override lateinit var backdropLayout: LinearLayout
    override lateinit var mainLayout: FrameLayout
    override lateinit var intervalChipGroup: ChipGroup
    override lateinit var intervalSpinner: Spinner
    override lateinit var intervalSpinnerLayout: View

    private lateinit var mOptionsMenu: Menu
    private val registeredFragments = SparseArray<Fragment>()
    private lateinit var listener: DateSelectionListener

    interface DateSelectionListener {
        fun onDateSelectionChange()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_statistic, container, false).apply {

            mContext = context

            backdropLayout = statistic_interval_layout
            mainLayout = statistic_main_layout
            intervalChipGroup = backdrop_interval_chip_group
            intervalSpinner = backdrop_time_selection_spinner
            intervalSpinnerLayout = backdrop_time_selection_layout


            val viewpager = statistic_viewpager
            viewpager.adapter = object : FragmentPagerAdapter(childFragmentManager) {

                override fun getCount(): Int {
                    return 3
                }

                override fun getItem(position: Int): Fragment {

                    return when (position) {

                        1 -> {
                            DistributionFragment().also {
                                listener = it
                            }
                        }

                        else -> DistributionFragment().also {
                            listener = it
                        }
                    }
                }
            }

            statistic_tabs.apply {
                addTab(newTab().setText(R.string.budget))
                addTab(newTab().setText(R.string.distribution))
                addTab(newTab().setText("Saving Potential"))
                addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {
                        viewpager.currentItem = tab?.position ?: 1
                    }

                    override fun onTabReselected(tab: TabLayout.Tab?) {}
                    override fun onTabUnselected(tab: TabLayout.Tab?) {}
                })
            }

            viewpager.addOnPageChangeListener(
                    TabLayout.TabLayoutOnPageChangeListener(statistic_tabs))

            viewpager.currentItem = 1

            initIntervalHelper()
            setHasOptionsMenu(true)
        }
    }

    override fun onIntervalSelected() {
        listener.onDateSelectionChange()
    }

    override fun onResume() {
        super.onResume()
        SimpleBudgetApp.handleRecurringEntries()
        backdropLayout.visibility = View.GONE
        if (::mOptionsMenu.isInitialized) updateOptionsMenu()
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

            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        mOptionsMenu = menu
        inflater.inflate(R.menu.menu_statictics, menu)
        updateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_statistic_time_interval -> {
            showIntervalLayout()
            true
        }
        R.id.menu_statistic_interval_done -> {
            hideIntervalLayout()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun updateOptionsMenu() {
        (requireActivity() as MainActivity).animateLayoutChanges()
        with(mOptionsMenu) {

            findItem(R.id.menu_statistic_time_interval)?.apply {
                isVisible = !backdropLayout.isVisible
                icon.alpha = 255
            }
            findItem(R.id.menu_statistic_interval_done)?.isVisible = backdropLayout.isVisible
        }
    }

    private fun hideIntervalLayout() {

        hideBackdrop {
            (activity as AppCompatActivity).supportActionBar?.elevation = 4f
        }

        with(mOptionsMenu) {
            MenuAnimator.setVisibility(findItem(R.id.menu_statistic_interval_done), false) {
                MenuAnimator.setVisibility(findItem(R.id.menu_statistic_time_interval), true)
            }
        }
    }

    private fun showIntervalLayout() {

        (activity as AppCompatActivity).supportActionBar?.elevation = 0f

        showBackdrop()

        (activity as MainActivity).setTitle(getString(R.string.select_interval))

        selectIntervalChip()
        backdropLayout.invalidate()

        with(mOptionsMenu) {
            MenuAnimator.setVisibility(findItem(R.id.menu_statistic_time_interval), false) {
                MenuAnimator.setVisibility(findItem(R.id.menu_statistic_interval_done), true)
            }
        }
    }


}
