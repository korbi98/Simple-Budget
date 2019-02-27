package com.korbi.simplebudget.logic

import android.content.Context
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bignerdranch.expandablerecyclerview.ChildViewHolder
import com.korbi.simplebudget.R
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
    private val dateFormatter = SimpleDateFormat("dd.MM.yy", Locale.US)

    init {
        expenseListening.setOnLongClickListener(){
            Toast.makeText(context, "test", Toast.LENGTH_LONG).show()

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


