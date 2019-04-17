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

package com.korbi.simplebudget.logic.model

import androidx.lifecycle.LiveData
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.korbi.simplebudget.utilities.*

import org.threeten.bp.LocalDate

interface ExpenseDao {

    @Query("")
    fun getExpensesByDate(firstDate: LocalDate, lastDate: LocalDate): LiveData<List<Expense>>

    @Query("SELECT * FROM $EXPENSE_TABLE WHERE " +
            "$COL_INTERVAL = $WEEKLY_ROOT or $COL_INTERVAL = $MONTHLY_ROOT")
    fun getRecurringExpenses(): LiveData<List<Expense>>

    @Query("SELECT * FROM $EXPENSE_TABLE WHERE $COL_ID = :id")
    fun getExpenseByID(id: Int): Expense

    @Query("SELECT * FROM $EXPENSE_TABLE ORDER BY $COL_DATE ASC LIMIT 1")
    fun getOldestDate(): LocalDate

    @Query("SELECT * FROM $EXPENSE_TABLE ORDER BY $COL_DATE DESC LIMIT 1")
    fun getNewestDate(): LocalDate

    @Insert
    fun addExpense(expense: Expense)

    @Update
    fun updateExpense(expense: Expense)

    @Delete
    fun deleteExpense(expense: Expense)

    @Query("")
    fun deleteExpenseByCategory(category: Category)

    @Query("")
    fun convertToNonRecurring(income: Expense)

    @Query("")
    fun deleteRecurringEntry(income: Expense)

    @Query("")
    fun migrateExpenses(oldCategory: Category, newCategory: Category)
}