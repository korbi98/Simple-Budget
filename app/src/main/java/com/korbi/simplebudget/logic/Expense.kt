package com.korbi.simplebudget.logic

import java.util.Date

// Class definition of the expense object which holds all the data of an expense. Apparently there
// is far less code involved than in java as setter and getter methods are automatically created
data class Expense(var id: Int,
                   var description: String,
                   var cost: Int,
                   var date: Date,
                   var category: String) {


    override fun equals(other: Any?): Boolean {
        if (this !== other) return false
        return (this.category == other.category && this.cost == other.cost &&
                this.description == other.description && this.date == other.date)
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + description.hashCode()
        result = 31 * result + cost
        result = 31 * result + date.hashCode()
        result = 31 * result + category.hashCode()
        return result
    }
}