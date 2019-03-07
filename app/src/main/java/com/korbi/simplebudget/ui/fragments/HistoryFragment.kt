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

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.RecyclerView

import com.korbi.simplebudget.R
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.adapters.HistoryAdapter
import android.view.*
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter
import com.korbi.simplebudget.MainActivity
import com.korbi.simplebudget.logic.DateHelper
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.logic.ExpenseViewHolder
import com.korbi.simplebudget.logic.HistoryEntry
import com.korbi.simplebudget.ui.*
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.TemporalAdjuster
import org.threeten.bp.temporal.TemporalAdjusters

import java.util.*


class HistoryFragment : androidx.fragment.app.Fragment(), ExpenseViewHolder.ExpenseAdapterListener,
                                                        FilterBottomSheet.OnFilterFragmentListener {

    private lateinit var historyRecycler: RecyclerView
    private lateinit var db: DBhandler
    private lateinit var historyAdapter: HistoryAdapter

    private var mActionMode: ActionMode? = null
    private var parentPosition = 1 // position to update when update expenses
    private var typeSelection = 0 //0 for both, 1 for expenses, 2 for income
    private var dateSelection = 3 //0 last 30 days, 1 last 90 days, 2 this year, 3 all time
    private lateinit var categorySelection: IntArray //1 if category selected 0 else
    private lateinit var mOptionsMenu: Menu

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        db = DBhandler.getInstance()
        categorySelection = IntArray(db.getAllCategories().size) { 1 }

        val rootview = inflater.inflate(R.layout.fragment_history, container, false)

        historyRecycler = rootview.findViewById(R.id.historyList)
        historyRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)

        historyRecycler.layoutManager = LinearLayoutManager(activity,
                                            RecyclerView.VERTICAL, false)
        historyRecycler.isNestedScrollingEnabled = false

        setHasOptionsMenu(true)
        return rootview
    }

    override fun onPause() {
        super.onPause()
        mActionMode?.finish()
        mActionMode = null
    }

    override fun onResume() {
        super.onResume()
        updateView(getHistoryEntries())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        mOptionsMenu = menu
        menu.clear()
        inflater.inflate(R.menu.menu_history, menu)
        val searchItem = menu.findItem(R.id.menu_history_search)
        val searchView: SearchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String?): Boolean {
                search()
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                search()
                return false
            }

            fun search() {
                historyAdapter = HistoryAdapter(performSearch(searchView.query.toString()), this@HistoryFragment)
                historyRecycler.adapter = historyAdapter
                historyAdapter.sort()
                historyAdapter.initializeSelectedItems()

                for (index in historyAdapter.parentList.indices) {
                    historyAdapter.expandParent(index)
                }
            }
        })

        searchView.setOnCloseListener {
            updateView(getHistoryEntries())
            false
        }
        updateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.menu_history_filter -> {

                val bundle = Bundle()
                bundle.putInt(TYPE_PREFILL, typeSelection)
                bundle.putInt(DATE_PREFILL, dateSelection)
                bundle.putIntArray(CATEGORY_PRESELECT, categorySelection)

                val filterFragment = FilterBottomSheet()
                filterFragment.arguments = bundle
                filterFragment.setListener(this)
                filterFragment.show(activity!!.supportFragmentManager, filterFragment.tag)
                true
            }

            R.id.menu_history_search -> {

                true
            }

            R.id.menu_history_filter_reset -> {
                onSelectionChanged(0, 3, IntArray(categorySelection.size){1})
                true
            }
            else -> false
        }
    }

    private fun getHistoryEntries(): MutableList<HistoryEntry> {

        val historyEntries = mutableListOf<HistoryEntry>()

        for (week in DateHelper.getInstance().getWeeks()) {

            val dateString = dateFormatter.format(week[0]) + " - " + dateFormatter.format(week[1])

            var expenses = db.getExpensesByDate(week[0], week[1])
            expenses = filterExpenses(expenses, typeSelection, dateSelection, categorySelection)

            historyEntries.add(HistoryEntry(expenses, dateString))
        }
        return historyEntries
    }

    private val mActionModeCallBack = object : ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
            val inflater: MenuInflater = mode.menuInflater
            inflater.inflate(R.menu.expenselistening_menu, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.expense_menu_edit -> {
                    updateSelected()
                    true
                }
                R.id.expense_menu_delete -> {
                    deleteSelected()
                    true
                }
                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            mActionMode = null
            historyAdapter.initializeSelectedItems()
            historyAdapter.notifyDataSetChanged()
            historyAdapter.resetCurrentIndex()
        }
    }

    private fun toggleSelection(parentPosition: Int, childPosition: Int) {

        if (mActionMode != null) {
            historyAdapter.toggleSelection(parentPosition, childPosition)


            val count = historyAdapter.getSelectedItemCount()
            if (count == 0) {
                mActionMode?.finish()
                mActionMode = null
            } else {
                val menu: Menu? = mActionMode!!.menu
                menu!!.findItem(R.id.expense_menu_edit).isVisible = count < 2 // hide edit option if more than one expense is selected
                mActionMode?.title = count.toString()
                mActionMode?.invalidate()
            }
        }
    }

    private fun deleteSelected(){
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setTitle(getString(R.string.delete_expenses_message))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    db.deleteExpenses(historyAdapter.getAndDeleteSelectedIndices())
                }
                .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                    dialog.cancel()
                }
                .show()
    }

    private fun updateSelected() {
        val indices = historyAdapter.getSingleSelectedExpenseIndex()
        val expenseToUpdate = historyAdapter.parentList[indices[0]].childList[indices[1]]
        val intent = Intent(context, AddExpenses::class.java)
        intent.putExtra(EXPENSE_INDEX, expenseToUpdate.id)
        intent.putExtra(EXPENSE_DESC, expenseToUpdate.description)
        intent.putExtra(EXPENSE_COST, expenseToUpdate.cost)
        intent.putExtra(EXPENSE_DATE, dateFormatter.format(expenseToUpdate.date))
        intent.putExtra(EXPENSE_CAT, expenseToUpdate.category)
        parentPosition = indices[0]
        startActivityForResult(intent, 1)
    }

    private fun filterExpenses(expenses: MutableList<Expense>,typeSelection: Int, dateSelection: Int,
                              categorySelection: IntArray) : MutableList<Expense> {

        // filter income or expense
        val typeFilteredList: List<Expense> = when (typeSelection) {
            1 -> expenses.filter { it.cost <= 0} // Expenses
            2 -> expenses.filter { it.cost > 0 } // Income
            else -> expenses
        }

        // filter date selection
        val currentDate = LocalDate.now()

        //TODO implement specific time range
        val dateFilteredList: List<Expense> = when (dateSelection) {
            0 -> {
                typeFilteredList.filter { it.date.isAfter(currentDate.minusDays(30)) }
            }
            1 -> {
                typeFilteredList.filter { it.date.isAfter(currentDate.minusDays(90)) }
            }
            2 -> {
                typeFilteredList.filter {
                    it.date.isAfter(currentDate.with(TemporalAdjusters.firstDayOfYear()))
                }
            }
            else -> typeFilteredList
        }

        val categoryFilteredList = dateFilteredList.filter {
            categorySelection[db.getAllCategories().indexOf(it.category)] == 1
        }

        return categoryFilteredList.toMutableList()
    }

    private fun performSearch(searchPhrase: String): MutableList<HistoryEntry> {
        val search = searchPhrase.toLowerCase()
        val searchedList = getHistoryEntries()

        for (hEntry in searchedList.iterator()) {
            val expenseIterator = hEntry.childList.iterator()
            for (expense in expenseIterator) {
                if (!expense.description.toLowerCase().contains(search)) {
                    expenseIterator.remove()
                }
            }
        }

        return searchedList
    }

    private fun updateView(hEntries: MutableList<HistoryEntry>) {
        historyAdapter = HistoryAdapter(hEntries, this)
        historyAdapter.setExpandCollapseListener(object : ExpandableRecyclerAdapter.
                                                                ExpandCollapseListener {

            override fun onParentCollapsed(parentPosition: Int) {
                (activity as MainActivity).expandedStateMap[parentPosition] = false
            }

            override fun onParentExpanded(parentPosition: Int) {
                (activity as MainActivity).expandedStateMap[parentPosition] = true
            }
        })

        historyRecycler.adapter = historyAdapter
        historyAdapter.sort()
        historyAdapter.initializeSelectedItems()
        updateExpandedStateMap()

        (activity as MainActivity).expandedStateMap.forEach {(position, state) ->
            if (state) historyAdapter.expandParent(position)
        }
    }

    override fun onSelectionChanged(type: Int, date: Int, categories: IntArray) {
        typeSelection = type
        dateSelection = date
        categorySelection = categories
        updateView(getHistoryEntries())
        updateOptionsMenu()
    }

    override fun onItemClicked(parentPosition: Int, childPosition: Int) {
        toggleSelection(parentPosition, childPosition)
    }

    override fun onItemLongClicked(parentPosition: Int, childPosition: Int, view: View) {

        when (mActionMode) {
            null -> {
                mActionMode = activity?.startActionMode(mActionModeCallBack)
                view.isSelected = true
            }
        }
        toggleSelection(parentPosition, childPosition)
    }

    private fun updateExpandedStateMap() {
        for (index in historyAdapter.parentList.indices) {
            if (!(activity as MainActivity).expandedStateMap.containsKey(index)) {
                (activity as MainActivity).expandedStateMap[index] = false
            }
        }
    }

    private fun updateOptionsMenu() {
        Log.d("test", "test")
        mOptionsMenu.findItem(R.id.menu_history_filter_reset).isVisible = typeSelection != 0 ||
                dateSelection != 3 ||
                !categorySelection.contentEquals(IntArray(categorySelection.size){1})
    }
}