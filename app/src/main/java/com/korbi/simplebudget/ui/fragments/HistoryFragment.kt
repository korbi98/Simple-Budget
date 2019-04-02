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
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter
import com.korbi.simplebudget.MainActivity
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.logic.DateHelper
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.logic.ExpenseViewHolder
import com.korbi.simplebudget.logic.HistoryEntry
import com.korbi.simplebudget.ui.*
import kotlinx.android.synthetic.main.fragment_history.view.*
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import org.threeten.bp.temporal.TemporalAdjusters
import java.util.*


class HistoryFragment : androidx.fragment.app.Fragment(), ExpenseViewHolder.ExpenseAdapterListener,
                                                        FilterBottomSheet.OnFilterFragmentListener,
                                                        HistoryAdapter.ClickRecurrentEntryListener {

    private lateinit var emptyMessage: TextView
    private lateinit var historyRecycler: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var mOptionsMenu: Menu

    private val db = DBhandler.getInstance()
    private var mActionMode: ActionMode? = null
    private var parentPosition = 1 // position to update when update expenses

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootview = inflater.inflate(R.layout.fragment_history, container, false)

        emptyMessage = rootview.history_fragment_empty_message
        historyRecycler = rootview.historyList.apply {
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            setHasFixedSize(true)
        }

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
        SimpleBudgetApp.handleRecurringEntries()
        updateView(getHistoryEntries((activity as MainActivity).typeSelection,
                                        (activity as MainActivity).dateSelection,
                                        (activity as MainActivity).fromDateSelection,
                                        (activity as MainActivity).toDateSelection,
                                        (activity as MainActivity).categorySelection))
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
                historyAdapter = HistoryAdapter(performSearch(searchView.query.toString()), this@HistoryFragment, this@HistoryFragment)
                historyRecycler.adapter = historyAdapter
                historyAdapter.sort()
                historyAdapter.initializeSelectedItems()

                for (index in historyAdapter.parentList.indices) {
                    historyAdapter.expandParent(index)
                }
            }
        })

        searchView.setOnSearchClickListener {
            mOptionsMenu.findItem(R.id.menu_history_filter_reset).isVisible = false
        }
        searchView.setOnCloseListener {
            updateView(getHistoryEntries((activity as MainActivity).typeSelection,
                    (activity as MainActivity).dateSelection,
                    (activity as MainActivity).fromDateSelection,
                    (activity as MainActivity).toDateSelection,
                    (activity as MainActivity).categorySelection))
            updateOptionsMenu()
            false
        }
        updateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.menu_history_filter -> {

                val bundle = Bundle()
                bundle.putInt(TYPE_PREFILL, (activity as MainActivity).typeSelection)
                bundle.putInt(DATE_PREFILL, (activity as MainActivity).dateSelection)
                bundle.putString(FROM_DATE_PRESELECT,
                        dateFormatter.format((activity as MainActivity).fromDateSelection))
                bundle.putString(TO_DATE_PRESELECT,
                        dateFormatter.format((activity as MainActivity).toDateSelection))
                bundle.putBooleanArray(CATEGORY_PRESELECT,
                        (activity as MainActivity).categorySelection)

                val filterFragment = FilterBottomSheet()
                filterFragment.arguments = bundle
                filterFragment.setListener(this)
                filterFragment.show(requireActivity().supportFragmentManager, filterFragment.tag)
                true
            }

            R.id.menu_history_filter_reset -> {
                onSelectionChanged(TYPE_BOTH, SELECT_ALL,
                        (activity as MainActivity).fromDateSelection,
                        (activity as MainActivity).toDateSelection,
                        BooleanArray((activity as MainActivity).categorySelection.size){true})
                true
            }
            else -> false
        }
    }

    private fun getHistoryEntries(type: Int,
                                  date: Int,
                                  fromDate: LocalDate,
                                  toDate: LocalDate,
                                  categories: BooleanArray): MutableList<HistoryEntry> {

        val historyEntries = mutableListOf<HistoryEntry>()

        val historyGrouping = SimpleBudgetApp.pref.getString(
                getString(R.string.settings_key_history_grouping), "1")

        when (historyGrouping) {
            MONTHLY_INTERVAL.toString() -> {
                for (month in DateHelper.getMonths()) {

                    val dateString =
                            month.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " +
                                    month.year.toString()

                    var expenses =
                            db.getExpensesByDate(month.atDay(1), month.atEndOfMonth())
                    expenses = filterExpenses(expenses, type, date, fromDate, toDate, categories)

                    historyEntries.add(HistoryEntry(expenses, dateString))
                }
            }

            WEEKLY_INTERVAL.toString() -> {
                for (week in DateHelper.getWeeks()) {

                    val dateString =
                            "${dateFormatter.format(week[0])} - ${dateFormatter.format(week[1])}"

                    var expenses = db.getExpensesByDate(week[0], week[1])
                    expenses = filterExpenses(expenses, type, date, fromDate, toDate, categories)

                    historyEntries.add(HistoryEntry(expenses, dateString))
                }
            }
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
            historyAdapter.resetCurrentIndex()
        }
    }

    private fun toggleSelection(parentPosition: Int, childPosition: Int) {

        mActionMode?.let {
            historyAdapter.toggleSelection(parentPosition, childPosition)

            val count = historyAdapter.getSelectedItemCount()
            if (count == 0) {
                it.finish()
                mActionMode = null
            } else {
                val menu: Menu? = it.menu
                menu?.findItem(R.id.expense_menu_edit)?.isVisible = count < 2 // hide edit option if more than one expense is selected
                it.title = count.toString()
                it.invalidate()
            }
        }
    }

    private fun deleteSelected(){
        with(AlertDialog.Builder(context)) {
            setTitle(getString(R.string.delete_expenses_message))
            setPositiveButton(getString(R.string.yes)) { _, _ ->
                db.deleteExpenses(historyAdapter.getAndDeleteSelectedIndices())
                updateWidget()
                mActionMode?.finish()
            }
            setNegativeButton(getString(R.string.no)) { dialog, _ ->
                dialog.cancel()
            }
            show()
        }
    }

    private fun updateSelected() {
        val indices = historyAdapter.getSingleSelectedExpenseIndex()
        val expenseToUpdate = historyAdapter.parentList[indices[0]].childList[indices[1]]

        parentPosition = indices[0]

        Intent(context, AddExpenses::class.java).run {
            putExtra(EXPENSE_INDEX, expenseToUpdate.id)
            putExtra(EXPENSE_DESC, expenseToUpdate.description)
            putExtra(EXPENSE_COST, expenseToUpdate.cost)
            putExtra(EXPENSE_DATE, dateFormatter.format(expenseToUpdate.date))
            putExtra(EXPENSE_CAT, expenseToUpdate.category.id)
            startActivityForResult(this, 1)
        }
    }

    private fun filterExpenses(expenses: MutableList<Expense>,
                               typeSelection: Int,
                               dateSelection: Int,
                               fromDate: LocalDate,
                               toDate: LocalDate,
                               categorySelection: BooleanArray) : MutableList<Expense> {

        // filter income or expense
        val typeFilteredList: List<Expense> = when (typeSelection) {
            TYPE_EXPENSE -> expenses.filter { it.cost <= 0} // Expenses
            TYPE_INCOME -> expenses.filter { it.cost > 0 } // Income
            else -> expenses
        }

        // filter date selection
        val currentDate = LocalDate.now()

        val dateFilteredList: List<Expense> = when (dateSelection) {
            SELECT_LAST30 -> {
                typeFilteredList.filter {
                    it.date.isAfter(currentDate.minusDays(31)) &&
                            it.date.isBefore(currentDate.plusDays(1))
                }
            }
            SELECT_LAST90 -> {
                typeFilteredList.filter {
                    it.date.isAfter(currentDate.minusDays(91)) &&
                            it.date.isBefore(currentDate.plusDays(1))
                }
            }
            SELECT_YEAR -> {
                typeFilteredList.filter {
                    it.date.isAfter(currentDate.with(TemporalAdjusters.firstDayOfYear()))&&
                            it.date.isBefore(currentDate.plusDays(1))
                }
            }
            SELECT_CUSTOM -> {
                typeFilteredList.filter {
                    it.date.isAfter(fromDate.minusDays(1)) &&
                            it.date.isBefore(toDate.plusDays(1))
                }
            }
            else -> typeFilteredList
        }

        val categoryFilteredList = dateFilteredList.filter {
            categorySelection[it.category.position]
        }

        return categoryFilteredList.toMutableList()
    }

    private fun performSearch(searchPhrase: String): MutableList<HistoryEntry> {
        val search = searchPhrase.toLowerCase()
        val searchedList = getHistoryEntries(TYPE_BOTH, SELECT_ALL,
                (activity as MainActivity).fromDateSelection,
                (activity as MainActivity).toDateSelection,
                BooleanArray((activity as MainActivity).categorySelection.size) { true })

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
        emptyMessage.visibility = when (hEntries.none { it.childList.isNotEmpty() }) {
            true -> View.VISIBLE
            false -> View.GONE
        }

        historyAdapter = HistoryAdapter(hEntries, this, this).apply {
            setExpandCollapseListener(object : ExpandableRecyclerAdapter.
            ExpandCollapseListener {

                override fun onParentCollapsed(parentPosition: Int) {
                    (activity as MainActivity).expandedStateMap[parentPosition] = false
                }

                override fun onParentExpanded(parentPosition: Int) {
                    (activity as MainActivity).expandedStateMap[parentPosition] = true
                }
            })
            sort()
            initializeSelectedItems()
        }
        historyRecycler.adapter = historyAdapter
        updateExpandedStateMap()

        for (index in historyAdapter.parentList.indices) {
            when ((activity as MainActivity).expandedStateMap[index]) {
                true -> historyAdapter.expandParent(index)
            }
        }
    }

    override fun onSelectionChanged(type: Int,
                                    date: Int,
                                    fromDate: LocalDate,
                                    toDate: LocalDate,
                                    categories: BooleanArray) {
        (activity as MainActivity).typeSelection = type
        (activity as MainActivity).dateSelection = date
        (activity as MainActivity).categorySelection = categories
        (activity as MainActivity).fromDateSelection = fromDate
        (activity as MainActivity).toDateSelection = toDate
        updateView(getHistoryEntries(type, date, fromDate, toDate, categories))
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

    override fun onClickRecurrentEntry(parentPosition: Int, childPosition: Int) {
        if (!SimpleBudgetApp.pref.getBoolean(getString(R.string.dont_show_again_key), false)) {
            with(AlertDialog.Builder(context)) {
                setTitle(R.string.this_is_recurrent_entry)
                setMessage(R.string.this_is_recurrent_entry_message)
                setPositiveButton(R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                }
                setNeutralButton(R.string.dont_show_again) {dialog, _ ->
                    with(SimpleBudgetApp.pref.edit()) {
                        putBoolean(getString(R.string.dont_show_again_key), true)
                        apply()
                    }
                    dialog.dismiss()
                }
                show()
            }
        }
    }

    private fun updateExpandedStateMap() {
        for (index in historyAdapter.parentList.indices) {
            if (!(activity as MainActivity).expandedStateMap.containsKey(index)) {
                (activity as MainActivity).expandedStateMap[index] = false
            }
        }
    }

    private fun updateOptionsMenu() {
        with(activity as MainActivity) {
            mOptionsMenu.findItem(R.id.menu_history_filter_reset).isVisible =
                    (typeSelection != TYPE_BOTH || dateSelection != SELECT_ALL ||
                            !categorySelection.none { !it })
        }
    }

    private fun updateWidget() {
        activity?.sendBroadcast(
                SimpleBudgetApp.updateWidgetIntent(requireContext(), requireActivity().application))
    }
}