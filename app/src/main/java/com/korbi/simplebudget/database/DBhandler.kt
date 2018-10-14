package com.korbi.simplebudget.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.korbi.simplebudget.logic.Expense
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

private const val DB_NAME = "ExpenseDatabase.db"
private const val DB_VERSION = 1
private const val EXPENSE_TABLE = "expenses"
private const val COL_ID = "_id"
private const val COL_COST = "cost"
private const val COL_DESCRIPTION = "description"
private const val COL_DATE = "date"

private const val CATEGORY_TABLE = "categories"
private const val COL_CATEGORY = "category"

class DBhandler(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {



    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)


    override fun onCreate(db: SQLiteDatabase) {
        val createExpenseTable = "CREATE TABLE $EXPENSE_TABLE ($COL_ID INTEGER PRIMARY KEY, " +
                                    "$COL_DESCRIPTION TEXT, $COL_COST INTEGER NOT NULL, " +
                                    "$COL_DATE TEXT)"

        db.execSQL(createExpenseTable)

        val createCategoryTable = "CREATE TABLE $CATEGORY_TABLE ( $COL_ID INTEGER PRIMARY KEY, " +
                                    "$COL_CATEGORY TEXT)"

        db.execSQL( createCategoryTable )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}


    fun addExpense(expense: Expense) {
        val db: SQLiteDatabase = this.writableDatabase

        val values = ContentValues()
        values.put(COL_DESCRIPTION, expense.description)
        values.put(COL_COST, expense.cost)
        values.put(COL_DATE, dateFormatter.format(expense.date))
        values.put(COL_CATEGORY, getCategoryId(expense.category))

        db.insert(CATEGORY_TABLE, null, values)
    }

    fun updateExpense(expense: Expense) {
        val db: SQLiteDatabase = this.writableDatabase

        val values = ContentValues()
        values.put(COL_DESCRIPTION, expense.description)
        values.put(COL_COST, expense.cost)
        values.put(COL_DATE, dateFormatter.format(expense.date))
        values.put(COL_CATEGORY, getCategoryId(expense.category))

        db.update(CATEGORY_TABLE, values, "$COL_ID = ?", arrayOf(expense.description))
    }

    fun addCategory(category: String) {

        val db = this.writableDatabase

        val values = ContentValues()
        values.put(COL_CATEGORY, category)

        db.insert(CATEGORY_TABLE, null, values)
        db.close()
    }

    private fun getCategoryId(category: String): Int{

        val query = "SELECT * FROM $CATEGORY_TABLE WHERE $COL_CATEGORY = '$category'"

        val db: SQLiteDatabase = this.writableDatabase
        val cursor = db.rawQuery(query, null)

        cursor?.moveToFirst()
        cursor.close()

        val id: Int
        id = if (cursor.count != 0) {
            cursor.getInt(0)
        } else {
            1
        }
        cursor.close()
        return id
    }

    fun getCategoryById(Id: Int): String {

        val query = "SELECT * FROM $CATEGORY_TABLE WHERE $COL_ID = $Id"

        val db: SQLiteDatabase = this.writableDatabase
        val cursor = db.rawQuery(query, null)

        cursor?.moveToFirst()
        val category = cursor.getString(1)
        cursor.close()
        return category
    }

    fun updateCategory(category: String, newName: String): Int {

        val db: SQLiteDatabase = this.writableDatabase
        val values = ContentValues()
        values.put(COL_CATEGORY, newName)

        return db.update(CATEGORY_TABLE, values, "$COL_CATEGORY = ?", arrayOf(category))
    }

    private fun deleteCategory(category: String) {
        //TODO implement logic.
        // If a category gets deleted the expenses that belong to he category should'nt be deleted,
        // but be migrated to another category
    }

    private fun parseDate(dateString: String): Date {
        var date: Date
        val fallback: Long = 0

        date = try {
            dateFormatter.parse(dateString)
        } catch (e: ParseException) {
            e.printStackTrace()
            // Error should be easy to spot as the Date is 01.01.1970 according to Unix time
            Date(0)
        }
        return date
    }
}
