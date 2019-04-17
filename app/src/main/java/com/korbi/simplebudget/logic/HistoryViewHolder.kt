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

package com.korbi.simplebudget.logic

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.bignerdranch.expandablerecyclerview.ParentViewHolder
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.logic.model.Expense
import kotlinx.android.synthetic.main.history_date_listening.view.*


class HistoryViewHolder(private val historyEntryView: View) : ParentViewHolder<HistoryEntry,
        Expense>(historyEntryView) {

    val context: Context = historyEntryView.context

    private val entryDate = historyEntryView.history_date_listing_date
    private val expandArrow = historyEntryView.history_date_listing_arrow_expand
    private val totalAmountView = historyEntryView.history_date_total_expense
    private val divider = historyEntryView.listDivider

    override fun collapseView() {
        super.collapseView()
        expandArrow.animate().rotation(0f).setDuration(100).start()
    }

    override fun expandView() {
        super.expandView()
        expandArrow.animate().rotation(90f).setDuration(100).start()
    }

    fun bind (week: String) {
        val totalAmount = parent.childList.sumBy { it.cost }
        entryDate.text = week
        totalAmountView.text = SimpleBudgetApp.createCurrencyString(totalAmount)

        totalAmountView.setTextColor(when {
            totalAmount < 0 -> ContextCompat.getColor(context,R.color.expenseColor)
            totalAmount > 0 -> ContextCompat.getColor(context,R.color.incomeColor)
            else -> ContextCompat.getColor(context,R.color.neutralColor)
        })

        divider.visibility = when (parentAdapterPosition) {
            0 -> View.GONE
            else -> View.VISIBLE
        }

        if (isExpanded) {
            expandArrow.animate().rotation(90f).setDuration(0).start()
        }

        historyEntryView.setOnClickListener {

            if (isExpanded) {
                collapseView()
            } else {
                expandView()
            }
        }
    }
}


