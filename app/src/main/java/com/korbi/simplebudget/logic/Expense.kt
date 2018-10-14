package com.korbi.simplebudget.logic

import java.util.Date


// Class definition of the expense object which holds all the data of an expense. Apparently there
// is far less code involved than in java as setter and getter methods are automatically created
data class Expense(var id: Int,
                   var description: String,
                   var cost: Int,
                   var date: Date,
                   var category: String)