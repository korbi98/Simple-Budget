package com.korbi.simplebudget.ui.fragments

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.RecyclerView

import com.korbi.simplebudget.R
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.HistoryAdapter
import android.view.*
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter
import com.korbi.simplebudget.MainActivity
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.logic.ExpenseViewHolder
import com.korbi.simplebudget.logic.HistoryEntry
import com.korbi.simplebudget.ui.*
import java.text.SimpleDateFormat
import java.util.*


class HistoryFragment : androidx.fragment.app.Fragment(), ExpenseViewHolder.ExpenseAdapterListener,
                                                        FilterBottomSheet.OnFilterFragmentListener {

    private lateinit var historyRecycler: RecyclerView
    private lateinit var db: DBhandler
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyEntries: MutableList<HistoryEntry>
    private var mActionMode: ActionMode? = null
    private var parentPosition = 1 // position to update when update expenses
    private var typeSelection = 0 //0 for both, 1 for expenses, 2 for income
    private var dateSelection = 3 //0 last 30 days, 1 last 90 days, 2 this year, 3 all time
    private lateinit var categorySelection: IntArray //1 if category selected 0 else
    private lateinit var mOptionsMenu: Menu

    private val dateFormatter = SimpleDateFormat("dd.MM.yy", Locale.US)

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

        historyEntries = getHistoryEntries()
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
        updateView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        mOptionsMenu = menu
        menu.clear()
        inflater.inflate(R.menu.menu_history, menu)
        val searchItem = menu.findItem(R.id.menu_history_search)
        val searchView: SearchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
        })
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

    private fun getWeekDates(week: Array<Int>): Array<Date> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, week[0]) // first entry of week array is year
        cal.set(Calendar.WEEK_OF_YEAR, week[1]) // second entry is number of week (1 to 52)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val firstDate = cal.time

        cal.set(Calendar.WEEK_OF_YEAR, week[1]+1)
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        val lastDate = cal.time

        return arrayOf(firstDate, lastDate)
    }

    private fun getWeeks(): MutableList<Array<Int>> {
        val expenses = db.getAllExpenses()
        val cal1 = Calendar.getInstance()
        val cal2 = Calendar.getInstance()
        val weeks = mutableListOf<Array<Int>>()

        if (!expenses.isEmpty()) {

            val oldestExpense: Expense? = expenses.minBy { it -> it.date }
            cal1.time = oldestExpense?.date
            val oldestYear = cal1.get(Calendar.YEAR)

            val latestExpense: Expense? = expenses.maxBy { it -> it.date }
            cal2.time = latestExpense?.date
            val latestYear = cal2.get(Calendar.YEAR)

            val oldestWeek = cal1.get(Calendar.WEEK_OF_YEAR)
            val latestWeek = cal2.get(Calendar.WEEK_OF_YEAR)

            if (latestYear == oldestYear) {

                for (week in latestWeek downTo oldestWeek) {
                    weeks.add(intArrayOf(oldestYear, week).toTypedArray())
                }

            } else {

                for (week in 52 downTo oldestWeek) {
                    weeks.add(intArrayOf(oldestYear, week).toTypedArray())
                }

                for (year in latestYear downTo oldestYear) {

                    if (year == latestYear) {

                        for (week in latestWeek downTo 1) {
                            weeks.add(intArrayOf(latestYear, week).toTypedArray())
                        }

                    } else {

                        for (week in 52 downTo 1) {
                            weeks.add(intArrayOf(year, week).toTypedArray())
                        }
                    }
                }
            }
        }
        return weeks
    }

    private fun getHistoryEntries(): MutableList<HistoryEntry> {

        val historyEntries = mutableListOf<HistoryEntry>()

        for (week in getWeeks()) {
            val weekDates = getWeekDates(week)
            val dateString = dateFormatter.format(weekDates[0]) + " - " +
                                                dateFormatter.format(weekDates[1])
            var expenses = db.getExpensesByDate(weekDates[0], weekDates[1])
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
        val cal = Calendar.getInstance()
        val dateFilteredList: List<Expense> = when (dateSelection) {
            0 -> {
                cal.add(Calendar.DATE,  - 30)
                typeFilteredList.filter { it.date.after(cal.time) }
            }
            1 -> {
                cal.add(Calendar.DATE, -90)
                typeFilteredList.filter { it.date.after(cal.time) }
            }
            2 -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                typeFilteredList.filter { it.date.after(cal.time) }
            }
            else -> typeFilteredList
        }

        val categoryFilteredList = dateFilteredList.filter {
            categorySelection[db.getAllCategories().indexOf(it.category)] == 1
        }

        return categoryFilteredList.toMutableList()
    }

    private fun updateView() {
        historyAdapter = HistoryAdapter(getHistoryEntries(), this)
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
        updateView()
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