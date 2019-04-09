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


import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.ChipGroup
import com.korbi.simplebudget.MainActivity
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.logic.IntervalSelectionBackdropHelper
import com.korbi.simplebudget.logic.MenuAnimator
import kotlinx.android.synthetic.main.fragment_statistic.*
import kotlinx.android.synthetic.main.interval_backdrop.view.*
import kotlinx.android.synthetic.main.fragment_statistic.view.*


class StatisticFragment : androidx.fragment.app.Fragment() {

    lateinit var intervalHelper: IntervalSelectionBackdropHelper
    private lateinit var backdropLayout: LinearLayout
    private lateinit var mOptionsMenu: Menu
    private lateinit var mainLayout: View
    private lateinit var intervalChipGroup: ChipGroup


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_statistic, container, false).apply {

            backdropLayout = statistic_interval_layout

            intervalHelper = IntervalSelectionBackdropHelper(
                    context = requireContext(),
                    backdropLayout = backdropLayout,
                    intervalChipGroup = backdrop_interval_chip_group,
                    intervalSpinner = backdrop_time_selection_spinner,
                    intervalLayout = backdrop_time_selection_layout,
                    mainLayout = statistic_main_layout
            )

            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (viewTreeObserver.isAlive)
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                    intervalHelper.deltaY = backdropLayout.height.toFloat()
                    backdropLayout.visibility = View.GONE
                }
            })

            setHasOptionsMenu(true)
        }
    }


    override fun onResume() {
        super.onResume()
        SimpleBudgetApp.handleRecurringEntries()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        mOptionsMenu = menu
        inflater.inflate(R.menu.menu_statictics, menu)
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

    private fun hideIntervalLayout() {

        intervalHelper.deltaY = backdropLayout.height.toFloat()

        statisticText.visibility = View.GONE
        statisticText.visibility = View.VISIBLE

        intervalHelper.hideBackdrop {
            (activity as AppCompatActivity).supportActionBar?.elevation = 4f
        }

        //statisticText.invalidate()

        with(mOptionsMenu) {
            MenuAnimator.setVisibility(findItem(R.id.menu_statistic_interval_done), false) {
                MenuAnimator.setVisibility(findItem(R.id.menu_statistic_time_interval), true)
            }
        }
    }


    private fun showIntervalLayout() {

        (activity as AppCompatActivity).supportActionBar?.elevation = 0f

        // for some reason, animation does not trigger when intervalTextView is not gone
        statisticText.visibility = View.GONE
        statisticText.visibility = View.VISIBLE

        intervalHelper.showBackdrop()

        (activity as MainActivity).setTitle(getString(R.string.select_interval))

        intervalHelper.selectIntervalChip()
        backdropLayout.invalidate()

        with(mOptionsMenu) {
            MenuAnimator.setVisibility(findItem(R.id.menu_statistic_time_interval), false) {
                MenuAnimator.setVisibility(findItem(R.id.menu_statistic_interval_done), true)
            }
        }
    }
}
