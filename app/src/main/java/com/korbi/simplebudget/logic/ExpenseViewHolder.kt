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
import android.content.res.TypedArray
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import com.bignerdranch.expandablerecyclerview.ChildViewHolder
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.logic.model.Expense
import com.korbi.simplebudget.utilities.NON_RECURRING
import kotlinx.android.synthetic.main.expense_listening.view.*
import org.threeten.bp.format.DateTimeFormatter

class ExpenseViewHolder(expenseListening: View, listener: ExpenseAdapterListener) :
                                    ChildViewHolder<Expense>(expenseListening) {

    val context:Context = expenseListening.context
    private val expenseDescription = expenseListening.expense_listening_description
    private val expenseDate = expenseListening.expense_listening_date
    private val expenseAmount = expenseListening.expense_listening_amount
    private val expenseIcon = expenseListening.expense_listening_icon

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")
    private val iconIdArray: TypedArray = context.resources.obtainTypedArray(R.array.category_icons)

    private var indicateRecurring = false

    init {
        expenseListening.setOnLongClickListener(){
            listener.onItemLongClicked(parentAdapterPosition, childAdapterPosition, it)
            true
        }
        expenseListening.setOnClickListener {
            listener.onItemClicked(parentAdapterPosition, childAdapterPosition)
        }

        indicateRecurring = SimpleBudgetApp.pref.getBoolean(
                context.getString(R.string.settings_key_show_recurring_symbol), true)
    }

    fun bind(expense: Expense) {
        expenseDate.text = dateFormatter.format(expense.date)
        expenseAmount.text =  SimpleBudgetApp.createCurrencyString(expense.cost)
        expenseIcon.setImageResource(iconIdArray.getResourceId(expense.category.icon, 0))

        if (expense.cost < 0) {
            expenseAmount.setTextColor(ContextCompat.getColor(context, R.color.expenseColor))
        } else {
            expenseAmount.setTextColor(ContextCompat.getColor(context, R.color.incomeColor))
        }

        if (expense.description.isEmpty()){
            expenseDescription.text = expense.category.name
        } else {
            expenseDescription.text = expense.description
        }

        if (expense.interval != NON_RECURRING && indicateRecurring) {
            val recurringSymbol = SpannableStringBuilder().bold { append(" \u27F3") }
            val enlargedBoldSymbol = SpannableString(recurringSymbol)
            enlargedBoldSymbol.setSpan(RelativeSizeSpan(1.3f), 1, 2, 0)

            expenseDescription.append(enlargedBoldSymbol)
        }
    }

    interface ExpenseAdapterListener {
        fun onItemClicked(parentPosition: Int, childPosition: Int)
        fun onItemLongClicked(parentPosition: Int, childPosition: Int, view: View)
    }
}


