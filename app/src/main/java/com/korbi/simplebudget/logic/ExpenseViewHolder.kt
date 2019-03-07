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
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bignerdranch.expandablerecyclerview.ChildViewHolder
import com.korbi.simplebudget.R
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ExpenseViewHolder(expenseListening: View, listener: ExpenseAdapterListener) :
                                    ChildViewHolder<Expense>(expenseListening) {

    val context:Context = expenseListening.context
    private val expenseDescription = expenseListening
                                        .findViewById<TextView>(R.id.expense_listening_description)
    private val expenseDate = expenseListening.findViewById<TextView>(R.id.expense_listening_date)
    private val expenseAmount = expenseListening
                                        .findViewById<TextView>(R.id.expense_listening_amount)
    private val decimalFormat = DecimalFormat("#.00")
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

    init {
        expenseListening.setOnLongClickListener(){
            listener.onItemLongClicked(parentAdapterPosition, childAdapterPosition, it)
            true
        }
        expenseListening.setOnClickListener {
            listener.onItemClicked(parentAdapterPosition, childAdapterPosition)
        }
    }

    fun bind(expense: Expense) {
        expenseDate.text = dateFormatter.format(expense.date)
        var amountText = decimalFormat.format(expense.cost.toFloat()/100).toString()
        amountText = "$amountText €"
        expenseAmount.text =  amountText

        if (expense.cost < 0) {
            expenseAmount.setTextColor(ContextCompat.getColor(context, R.color.expenseColor))
        } else {
            expenseAmount.setTextColor(ContextCompat.getColor(context, R.color.incomeColor))
        }

        if (expense.description.isEmpty()){
            expenseDescription.text = expense.category
        } else {
            expenseDescription.text = expense.description
        }
    }



    interface ExpenseAdapterListener {
        fun onItemClicked(parentPosition: Int, childPosition: Int)
        fun onItemLongClicked(parentPosition: Int, childPosition: Int, view: View)
    }
}


