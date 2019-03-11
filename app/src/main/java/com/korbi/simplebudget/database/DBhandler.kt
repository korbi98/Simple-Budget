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
import com.korbi.simplebudget.logic.Category
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.logic.RecurrentEntry
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import kotlin.collections.ArrayList

private const val DB_NAME = "ExpenseDatabase.db"
private const val DB_VERSION = 3
private const val EXPENSE_TABLE = "expenses"

private const val COL_ID = "_id"
private const val COL_COST = "cost"
private const val COL_DESCRIPTION = "description"
private const val COL_DATE = "date"

private const val CATEGORY_TABLE = "categories"
private const val COL_CATEGORY = "category"
private const val COL_DRAWABLE = "drawable"
private const val COL_POSITION = "position"

private const val RECURRING_TABLE = "regular"
private const val COL_INTERVAL = "interval"
private const val COL_INTERVAL_DATE = "interval_date"
private const val COL_STARTING_FROM = "starting_from"

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
                                    "$COL_CATEGORY TEXT, $COL_DRAWABLE INTEGER NOT NULL DEFAULT 0, " +
                                    "$COL_POSITION INTEGER NOT NULL DEFAULT 0)"

        db.execSQL( createCategoryTable )

        val values = ContentValues()
        for ((index, category) in defaultCategories.withIndex()) {
            values.put(COL_CATEGORY, category)
            values.put(COL_DRAWABLE, index)
            values.put(COL_POSITION, index)
            db.insert(CATEGORY_TABLE, null, values)
        }

        val createRecurringTable = "CREATE TABLE $RECURRING_TABLE " +
                "($COL_ID INTEGER PRIMARY KEY, " +
                "$COL_DESCRIPTION TEXT, " +
                "$COL_COST INTEGER, " +
                "$COL_INTERVAL INTEGER, " +
                "$COL_INTERVAL_DATE TEXT, " +
                "$COL_STARTING_FROM DATE)"
        db.execSQL(createRecurringTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        when (newVersion) {
            2 -> {
                db.execSQL("ALTER TABLE $CATEGORY_TABLE ADD COLUMN "
                        + "$COL_DRAWABLE INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE $CATEGORY_TABLE ADD COLUMN "
                        + "$COL_POSITION INTEGER NOT NULL DEFAULT 0")

                db.delete(CATEGORY_TABLE, "1", null)

                val values = ContentValues()
                for ((index, category) in defaultCategories.withIndex()) {
                    values.put(COL_CATEGORY, category)
                    values.put(COL_DRAWABLE, index)
                    values.put(COL_POSITION, index)
                    db.insert(CATEGORY_TABLE, null, values)
                }
            }
            3 -> {
                val createRecurringTable = "CREATE TABLE $RECURRING_TABLE " +
                                                        "($COL_ID INTEGER PRIMARY KEY, " +
                                                        "$COL_DESCRIPTION TEXT, " +
                                                        "$COL_COST INTEGER, " +
                                                        "$COL_INTERVAL INTEGER, " +
                                                        "$COL_INTERVAL_DATE TEXT, " +
                                                        "$COL_STARTING_FROM DATE)"
                db.execSQL(createRecurringTable)
            }
        }
    }

    fun addExpense(expense: Expense) {
        val db = this.writableDatabase

        val values = ContentValues()
        values.put(COL_DESCRIPTION, expense.description)
        values.put(COL_COST, expense.cost)
        values.put(COL_DATE, dateFormatter.format(expense.date))
        values.put(COL_CATEGORY, getCategoryId(expense.category))

        db.insert(EXPENSE_TABLE, null, values)
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
        val db = this.writableDatabase

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
        db.delete(EXPENSE_TABLE, query.append(")").toString(),
                                        indices.toArray<String>(arrayOfNulls(indices.size)))
    }

    fun deleteExpenesByCategory(category: Category) {
        val db = this.writableDatabase
        db.delete(EXPENSE_TABLE, "$COL_CATEGORY = ?", arrayOf(category.id.toString()))
    }

    fun migrateExpenses(oldCategory: Category, newCategory: Category) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_CATEGORY, newCategory.id)
        db.update(EXPENSE_TABLE, values, "$COL_CATEGORY = ?",
                arrayOf(oldCategory.id.toString()))
    }

    fun addCategory(category: Category) {

        val db = this.writableDatabase

        val values = ContentValues()
        values.put(COL_CATEGORY, category.name)
        values.put(COL_DRAWABLE, category.icon)
        values.put(COL_POSITION, category.position)

        db.insert(CATEGORY_TABLE, null, values)
    }

    private fun getCategoryId(category: Category): Int{

        val name = category.name
        val query = "SELECT * FROM $CATEGORY_TABLE WHERE $COL_CATEGORY = '$name'"

        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)

        cursor?.moveToFirst()

        val id: Int
        id = when {
            cursor.count != 0 -> cursor.getInt(0)
            else -> 1
        }
        cursor.close()
        return id
    }

    fun getCategoryById(Id: Int): Category {

        val query = "SELECT * FROM $CATEGORY_TABLE WHERE $COL_ID = $Id"

        val db = this.writableDatabase
        val cursor = db.rawQuery(query, null)

        cursor?.moveToFirst()
        val id = cursor.getInt(0)
        val name = cursor.getString(1)
        val drawable = cursor.getInt(2)
        val position = cursor.getInt(3)
        cursor.close()
        return Category(id, name, drawable, position)
    }

    fun getAllCategories(): MutableList<Category> {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT * FROM $CATEGORY_TABLE", null)
        val categories = mutableListOf<Category>()

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(0)
                val name = cursor.getString(1)
                val drawable = cursor.getInt(2)
                val position = cursor.getInt(3)
                categories.add(Category(id, name, drawable, position))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return categories
    }

    fun updatePosition(category: Category, newPos: Int) {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_POSITION, newPos)
        db.update(CATEGORY_TABLE, values, "$COL_ID = ?", arrayOf(category.id.toString()))
    }

    fun updateCategory(category: Category): Int {

        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_CATEGORY, category.name)
        values.put(COL_DRAWABLE, category.icon)
        values.put(COL_POSITION, category.position)

        return db.update(CATEGORY_TABLE, values, "$COL_ID = ?",
                arrayOf(category.id.toString()))
    }

    fun deleteCategory(category: Category): Int {
        val db = this.writableDatabase
        val query = "$COL_ID = ${category.id}"
        return db.delete(CATEGORY_TABLE, query, null)
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

    fun getLatestCategoryID(): Int {
        val latestID: Int
        val db = this.writableDatabase

        val cursor = db.rawQuery("SELECT max($COL_ID) FROM $CATEGORY_TABLE", null)
        cursor.moveToFirst()
        latestID = cursor.getInt(0)

        cursor.close()
        return latestID
    }

    fun getAllRecurringEntries(): MutableList<RecurrentEntry> {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT * FROM $RECURRING_TABLE", null)
        val recurring = mutableListOf<RecurrentEntry>()

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(0)
                val name = cursor.getString(1)
                val amount = cursor.getInt(2)
                val interval = cursor.getInt(3)
                val intervalDate = cursor.getString(4)
                val startFrom = LocalDate.parse(cursor.getString(5))
                recurring.add(RecurrentEntry(id, name, amount, interval, intervalDate, startFrom))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return recurring
    }

    fun updateRecurringEntry(entry: RecurrentEntry): Int {

        val db = this.writableDatabase
        val values = ContentValues()
        values.put(COL_DESCRIPTION, entry.name)
        values.put(COL_COST, entry.amount)
        values.put(COL_INTERVAL, entry.interval)
        values.put(COL_INTERVAL_DATE, entry.intervalDate)
        values.put(COL_STARTING_FROM, dateFormatter.format(entry.startingFrom))

        return db.update(RECURRING_TABLE, values, "$COL_ID = ?",
                arrayOf(entry.id.toString()))
    }

    fun deleteRecurringEntry(entry: RecurrentEntry): Int {
        val db = this.writableDatabase
        val query = "$COL_ID = ${entry.id}"
        return db.delete(RECURRING_TABLE, query, null)
    }

    fun getLatestRecurringEntryID(): Int {
        val latestID: Int
        val db = this.writableDatabase

        val cursor = db.rawQuery("SELECT max($COL_ID) FROM $RECURRING_TABLE", null)
        cursor.moveToFirst()
        latestID = cursor.getInt(0)

        cursor.close()
        return latestID
    }
}
