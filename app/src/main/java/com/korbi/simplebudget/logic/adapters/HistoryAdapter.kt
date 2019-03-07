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

package com.korbi.simplebudget.logic.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bignerdranch.expandablerecyclerview.ExpandableRecyclerAdapter
import com.korbi.simplebudget.R
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.logic.ExpenseViewHolder
import com.korbi.simplebudget.logic.HistoryEntry
import com.korbi.simplebudget.logic.HistoryViewHolder
import java.util.*


class HistoryAdapter(private val historyEntries: MutableList<HistoryEntry>,
                     private val listener: ExpenseViewHolder.ExpenseAdapterListener) :
                                            ExpandableRecyclerAdapter<HistoryEntry, Expense,
                                                    HistoryViewHolder, ExpenseViewHolder>(historyEntries) {

    private var currentSelectedChild = -1
    private var currentSelectedParent = -1
    private val selectedItems = mutableListOf<MutableList<Boolean>>()


    override fun onCreateParentViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.history_date_listening, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onCreateChildViewHolder(childViewGroup: ViewGroup, viewType: Int):
            ExpenseViewHolder {
        val view = LayoutInflater.from(childViewGroup.context)
                .inflate(R.layout.expense_listening, childViewGroup, false)
        return ExpenseViewHolder(view, listener)
    }

    override fun onBindParentViewHolder(parentViewHolder: HistoryViewHolder,
                                        position: Int, parent: HistoryEntry) {
        parentViewHolder.bind(historyEntries[position].getDateString())

        if (parent.childList.isNullOrEmpty()) {
            parentViewHolder.itemView.visibility = View.GONE
            parentViewHolder.itemView.layoutParams = RecyclerView.LayoutParams(0,0)
        } else {
            parentViewHolder.itemView.visibility = View.VISIBLE
            parentViewHolder.itemView.layoutParams = RecyclerView.
                                            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                    ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onBindChildViewHolder(childViewHolder: ExpenseViewHolder, parentPosition: Int,
                                       childPosition: Int, child: Expense) {
        childViewHolder.bind(historyEntries[parentPosition].childList[childPosition])

        if (selectedItems[parentPosition][childPosition]) {
            childViewHolder.itemView.background = childViewHolder.context
                                                    .getDrawable(android.R.color.darker_gray)
        } else {
            val attrs = IntArray(1){R.attr.selectableItemBackground}
            val typedArray = childViewHolder.context.obtainStyledAttributes(attrs)
            val backgroundResource = typedArray.getResourceId(0, 0)
            childViewHolder.itemView.setBackgroundResource(backgroundResource)
            typedArray.recycle()
        }
    }

    fun toggleSelection(parentPosition: Int, childPosition: Int) {
        selectedItems[parentPosition][childPosition] = !selectedItems[parentPosition][childPosition]
        currentSelectedParent = parentPosition
        currentSelectedChild = childPosition
        notifyChildChanged(parentPosition, childPosition)
    }

    fun getSelectedItemCount(): Int {
        var count = 0
        for (bool_array in selectedItems) {
            count += bool_array.filter{b -> b}.size
        }
        return count
    }

    fun resetCurrentIndex() {
        currentSelectedChild = -1
        currentSelectedParent = -1
    }

    fun initializeSelectedItems() {
        selectedItems.clear()
        for ((hIndex, hEntry) in historyEntries.withIndex()) {
            selectedItems.add(hIndex, mutableListOf())
            for (eIndex in hEntry.childList.indices) {
                selectedItems[hIndex].add(eIndex, false)
            }
        }
    }

    fun getAndDeleteSelectedIndices(): ArrayList<String> {
        val indices = arrayListOf<String>()

        for ((eIndex ,entry) in selectedItems.withIndex()) {
            var sCounter = 0
            for ((sIndex, state) in entry.withIndex()) {
                if (state) {
                    indices.add(historyEntries[eIndex].childList[sIndex - sCounter].id.toString())
                    historyEntries[eIndex].childList.removeAt(sIndex - sCounter)
                    notifyChildRemoved(eIndex, sIndex-sCounter)
                    notifyParentChanged(eIndex)
                    sCounter++
                }
            }
        }
        initializeSelectedItems()
        return indices
    }

    fun getSingleSelectedExpenseIndex(): Array<Int> {
        return arrayOf(currentSelectedParent, currentSelectedChild)
    }

    fun sort() {
        for (entry in historyEntries) {
            entry.childList.sortByDescending { it.date }
        }
    }
}
