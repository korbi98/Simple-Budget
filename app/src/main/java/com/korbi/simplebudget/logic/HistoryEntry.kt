package com.korbi.simplebudget.logic

import com.bignerdranch.expandablerecyclerview.model.Parent
import com.korbi.simplebudget.logic.Expense

class HistoryEntry(private val expenses: MutableList<Expense>, private val dateString: String) :
                                                                                    Parent<Expense>{

    override fun getChildList(): MutableList<Expense> {
        return expenses
    }

    override fun isInitiallyExpanded(): Boolean {
        return false
    }

    fun getDateString(): String {
        return dateString
    }
}
