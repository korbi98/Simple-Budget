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
import android.view.*
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.util.containsKey
import androidx.core.util.set
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.ExpenseViewHolder
import com.korbi.simplebudget.logic.HistoryEntry
import com.korbi.simplebudget.logic.HistoryHelper
import com.korbi.simplebudget.logic.adapters.HistoryAdapter
import com.korbi.simplebudget.ui.*
import com.korbi.simplebudget.utilities.EXPENSE_INDEX
import kotlinx.android.synthetic.main.fragment_history.view.*
import org.threeten.bp.LocalDate


class HistoryFragment : androidx.fragment.app.Fragment(), ExpenseViewHolder.ExpenseAdapterListener,
                                                        FilterBottomSheet.OnFilterFragmentListener,
                                                        HistoryAdapter.ClickRecurrentEntryListener {

    private lateinit var emptyMessage: TextView
    private lateinit var historyRecycler: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var mOptionsMenu: Menu
    private var historyEntries = mutableListOf<HistoryEntry>()

    private val db = DBhandler.getInstance()
    private var mActionMode: ActionMode? = null
    private var parentPosition = 1 // position to update when update expenses

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootview = inflater.inflate(R.layout.fragment_history, container, false)

        emptyMessage = rootview.history_fragment_empty_message

        historyAdapter = HistoryAdapter(historyEntries, this, this).apply {
            setExpandCollapseListener(object : ExpandableRecyclerAdapter.ExpandCollapseListener {

                override fun onParentCollapsed(parentPosition: Int) {
                    HistoryHelper.expandedStateMap[parentPosition] = false
                }

                override fun onParentExpanded(parentPosition: Int) {
                    HistoryHelper.expandedStateMap[parentPosition] = true
                }
            })
        }

        historyRecycler = rootview.historyList.apply {
            layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
            setHasFixedSize(true)
            adapter = historyAdapter
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

        HistoryHelper.run {
            if (categorySelection.size != db.getAllCategories().size) {
                categorySelection = BooleanArray(db.getAllCategories().size) { true }
            }
            updateView(getHistoryEntries(typeSelection, dateSelection, fromDateSelection,
                    toDateSelection, categorySelection, true))
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (hidden) {
            mActionMode?.finish()
            mActionMode = null
        }
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
                (requireActivity() as MainActivity).animateLayoutChanges(200)
                setData(performSearch(searchView.query.toString()))
                historyAdapter.run {
                    sort()
                    initializeSelectedItems()
                    notifyParentDataSetChanged(false)
                }

                for (index in historyAdapter.parentList.indices) {
                    historyAdapter.expandParent(index)
                }
            }
        })

        searchView.setOnSearchClickListener {
            mOptionsMenu.findItem(R.id.menu_history_filter_reset).isVisible = false
        }
        searchView.setOnCloseListener {
            (requireActivity() as MainActivity).animateLayoutChanges()

           HistoryHelper.run {
                updateView(getHistoryEntries(typeSelection, dateSelection, fromDateSelection,
                        toDateSelection, categorySelection, true))
            }
            updateOptionsMenu()
            false
        }
        (requireActivity() as MainActivity).animateLayoutChanges()
        updateOptionsMenu()
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_history_filter -> {

            HistoryHelper.createPrefilledFilterSheet()
                    .also { it.setListener(this) }.show(requireFragmentManager(), tag)
            true
        }

        R.id.menu_history_filter_reset -> {

            (requireActivity() as MainActivity).animateLayoutChanges()
            onSelectionChanged(TYPE_BOTH, SELECT_ALL,
                                HistoryHelper.fromDateSelection,
                                HistoryHelper.toDateSelection,
                                BooleanArray(HistoryHelper.categorySelection.size){true})
            true
        }
        else -> super.onOptionsItemSelected(item)
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

    private fun setData(data: MutableList<HistoryEntry>) {
        historyEntries.clear()
        historyEntries.addAll(data)
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
                mActionMode?.finish()
                with(activity as MainActivity){
                    dashboard?.let {
                        it.resetIntervalSelection()
                        it.setupTimeSelectionSpinner()
                    }
                    statistics?.let {
                        it.resetIntervalSelection()
                        it.setupTimeSelectionSpinner()
                    }
                }

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
            startActivityForResult(this, 1)
        }
    }

    private fun performSearch(searchPhrase: String): MutableList<HistoryEntry> {
        val search = searchPhrase.toLowerCase()
        val searchedList = HistoryHelper.getHistoryEntries(TYPE_BOTH, SELECT_ALL,
                HistoryHelper.fromDateSelection,
                HistoryHelper.toDateSelection,
                BooleanArray(HistoryHelper.categorySelection.size) { true })

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

        setData(hEntries)
        historyAdapter.run {
            sort()
            initializeSelectedItems()
            notifyParentDataSetChanged(false)
        }

        updateExpandedStateMap()

        for (index in historyAdapter.parentList.indices) {
            when (HistoryHelper.expandedStateMap[index]) {
                true -> historyAdapter.expandParent(index)
            }
        }
    }

    override fun onSelectionChanged(type: Int,
                                    date: Int,
                                    fromDate: LocalDate,
                                    toDate: LocalDate,
                                    categories: BooleanArray) {
        HistoryHelper.run {
            typeSelection = type
            dateSelection = date
            categorySelection = categories
            fromDateSelection = fromDate
            toDateSelection = toDate
            updateView(getHistoryEntries(type, date, fromDate, toDate, categories))
        }
        updateOptionsMenu()
    }

    override fun onItemClicked(parentPosition: Int, childPosition: Int) {
        toggleSelection(parentPosition, childPosition)
    }

    override fun onItemLongClicked(parentPosition: Int, childPosition: Int, view: View) {

        when (mActionMode) {
            null -> {
                mActionMode = (activity as AppCompatActivity)
                        .startSupportActionMode(mActionModeCallBack)
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
            if (!HistoryHelper.expandedStateMap.containsKey(index)) {
                HistoryHelper.expandedStateMap[index] = false
            }
        }
    }

    private fun updateOptionsMenu() {
        mOptionsMenu.findItem(R.id.menu_history_filter_reset).isVisible =
                        (HistoryHelper.typeSelection != TYPE_BOTH ||
                        HistoryHelper.dateSelection != SELECT_ALL ||
                        !HistoryHelper.categorySelection.none { !it })
    }
}