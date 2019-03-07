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

package com.korbi.simplebudget.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.korbi.simplebudget.logic.Expense
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val DB_NAME = "ExpenseDatabase.db"
private const val DB_VERSION = 1
private const val EXPENSE_TABLE = "expenses"
private const val COL_ID = "_id"
private const val COL_COST = "cost"
private const val COL_DESCRIPTION = "description"
private const val COL_DATE = "date"

private const val CATEGORY_TABLE = "categories"
private const val COL_CATEGORY = "category"

class DBhandler(context: Context, private val defaultCategories: Array<String>) :
                                    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    companion object {

        private var instance: DBhandler? = null

        fun createInstance(context: Context, defaultCategories: Array<String>) {
            if (instance == null) {
                instance = DBhandler(context, defaultCategories)
            }
        }

        fun getInstance(): DBhandler {
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createExpenseTable = "CREATE TABLE $EXPENSE_TABLE ($COL_ID INTEGER PRIMARY KEY, " +
                                    "$COL_DESCRIPTION TEXT, $COL_COST INTEGER NOT NULL, " +
                                    "$COL_DATE DATE, $COL_CATEGORY INTEGER NOT NULL)"

        db.execSQL(createExpenseTable)

        val createCategoryTable = "CREATE TABLE $CATEGORY_TABLE ( $COL_ID INTEGER PRIMARY KEY, " +
                                    "$COL_CATEGORY TEXT)"

        db.execSQL( createCategoryTable )

        val values = ContentValues()

        for (category in defaultCategories) {
            values.put(COL_CATEGORY, category)
            db.insert(CATEGORY_TABLE, null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun addExpense(expense: Expense) {
        val db: SQLiteDatabase = this.writableDatabase

        val values = ContentValues()
        values.put(COL_DESCRIPTION, expense.description)
        values.put(COL_COST, expense.cost)
        values.put(COL_DATE, dateFormatter.format(expense.date))
        values.put(COL_CATEGORY, getCategoryId(expense.category))

        db.insert(EXPENSE_TABLE, null, values)
    }

    fun getAllExpenses(): MutableList<Expense> {
        val expenses = mutableListOf<Expense>()
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT * FROM $EXPENSE_TABLE", null)

        if (cursor.moveToFirst()) {
            do {
                val expense = Expense(cursor.getInt(0),
                                    cursor.getString(1),
                                    cursor.getInt(2),
                                    LocalDate.parse(cursor.getString(3)),
                                    getCategoryById(cursor.getInt(4)))

                expenses.add(expense)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return expenses
    }

    fun getExpensesByDate(firstDate: LocalDate, lastDate: LocalDate): MutableList<Expense> {
        val expenses = mutableListOf<Expense>()
        val db = this.writableDatabase
        val firstDateStr = dateFormatter.format(firstDate)
        val lastDateStr = dateFormatter.format(lastDate)

        val query = "SELECT * FROM $EXPENSE_TABLE WHERE $COL_DATE BETWEEN '$firstDateStr' " +
                        "AND '$lastDateStr'"

        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val expense = Expense(cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getInt(2),
                        LocalDate.parse(cursor.getString(3)),
                        getCategoryById(cursor.getInt(4)))

                expenses.add(expense)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return expenses
    }

    fun getOldestDate(): LocalDate {
        val db = this.writableDatabase
        val query = "SELECT * FROM $EXPENSE_TABLE ORDER BY $COL_DATE ASC LIMIT 1"
        val cursor = db.rawQuery(query, null)

        val oldestDate = if (cursor.moveToFirst()) {
            LocalDate.parse(cursor.getString(3))
        } else {
            LocalDate.now()
        }
        cursor.close()

        return oldestDate
    }

    fun getNewestDate(): LocalDate {
        val db = this.writableDatabase
        val query = "SELECT * FROM $EXPENSE_TABLE ORDER BY $COL_DATE DESC LIMIT 1"
        val cursor = db.rawQuery(query, null)

        val newestDate = if (cursor.moveToFirst()) {
            LocalDate.parse(cursor.getString(3))
        } else {
            LocalDate.now()
        }
        cursor.close()

        return newestDate
    }

    fun updateExpense(expense: Expense) {
        val db: SQLiteDatabase = this.writableDatabase

        val values = ContentValues()
        values.put(COL_DESCRIPTION, expense.description)
        values.put(COL_COST, expense.cost)
        values.put(COL_DATE, dateFormatter.format(expense.date))
        values.put(COL_CATEGORY, getCategoryId(expense.category))

        db.update(EXPENSE_TABLE, values, "$COL_ID = ?", arrayOf(expense.id.toString()))
    }

    fun deleteExpenses(indices: ArrayList<String>) {
        val db = this.writableDatabase
        val query = StringBuilder("$COL_ID IN (?")
        for (i in indices.indices - 1) {
            query.append(",?")
        }
        val count = db.delete(EXPENSE_TABLE, query.append(")").toString(),
                                        indices.toArray<String>(arrayOfNulls(indices.size)))
        Log.d("test", count.toString())
    }

    fun addCategory(category: String) {

        val db = this.writableDatabase

        val values = ContentValues()
        values.put(COL_CATEGORY, category)

        db.insert(CATEGORY_TABLE, null, values)
    }

    private fun getCategoryId(category: String): Int{

        val query = "SELECT * FROM $CATEGORY_TABLE WHERE $COL_CATEGORY = '$category'"

        val db: SQLiteDatabase = this.writableDatabase
        val cursor = db.rawQuery(query, null)

        cursor?.moveToFirst()

        val id: Int
        id = if (cursor.count != 0) {
            cursor.getInt(0)
        } else {
            1
        }
        cursor.close()
        return id
    }

    private fun getCategoryById(Id: Int): String {

        val query = "SELECT * FROM $CATEGORY_TABLE WHERE $COL_ID = $Id"

        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)

        cursor?.moveToFirst()
        val category = cursor.getString(1)
        cursor.close()
        return category
    }

    fun getAllCategories(): MutableList<String> {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT * FROM $CATEGORY_TABLE", null)
        val categories = mutableListOf<String>()

        if (cursor.moveToFirst()) {
            do {
                categories.add(cursor.getString(1))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return categories
    }

    fun updateCategory(category: String, newName: String): Int {

        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_CATEGORY, newName)

        return db.update(CATEGORY_TABLE, values, "$COL_CATEGORY = ?", arrayOf(category))
    }

    private fun deleteCategory(category: String) {
        //TODO implement logic.
        // If a category gets deleted the expenses that belong to he category should'nt be deleted,
        // but be migrated to another category
    }


    fun getLatestID(): Int {
        val latestID: Int
        val db = this.writableDatabase

        val cursor = db.rawQuery("SELECT max($COL_ID) FROM $EXPENSE_TABLE", null)
        cursor.moveToFirst()
        latestID = cursor.getInt(0)

        cursor.close()
        return latestID
    }
}
