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

import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.model.Category
import com.korbi.simplebudget.logic.model.Expense
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.io.*

import java.lang.StringBuilder
import java.text.ParseException

const val HEADER_STRING = "### DO NOT MODIFY THIS FILE. Import file from settings in Simple Budget app. ###"
const val CATEGORY_HEADER = "### categories ###"
const val EXPENSE_HEADER = "### expenses ###"

object ImportExportHelper {

    private val db = DBhandler.getInstance()

    fun writeCSV(directory: String): Boolean {

        val dateString = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        val filename = "SimpleBudget_export_$dateString.csv"

        return try {
            File(directory, filename).writeText(createDataString())
            true
        } catch (e: IOException) {
            false
        }
    }

    fun readCSV(directory: String): List<List<Int>>? {

        val file = File(directory)
        val data = file.readLines()
        return if (data[0] == HEADER_STRING && data[1] == CATEGORY_HEADER) {
            data.run { importExpensesFromCSV(subList(2, size)) }
        }
        else null // invalid file
    }

    private fun importExpensesFromCSV(data: List<String>): List<List<Int>> {
        val corruptedLines = mutableListOf<Int>()
        val missingCategories = mutableListOf<Int>()

        val importedExpenses = mutableListOf<Expense>()
        val categoryImport = importCategoriesFromCSV(data)

        val fallbackCategory = Category(db.getLatestCategoryID(),
                                    SimpleBudgetApp.res.getString(R.string.fallback_category_name),
                                        45, db.getAllCategories().size, 0, 0)

        categoryImport.second.forEachIndexed { lineNumber, line ->

            val valueList = line.split(",")

            if (valueList.size == 6) {

                val id = valueList[0].toIntOrNull()
                val description = valueList[1]
                val cost = valueList[2].toIntOrNull()
                val date: LocalDate? = try {
                    LocalDate.parse(valueList[3])
                } catch (e: ParseException) {
                    null
                }
                val categoryId = valueList[4].toIntOrNull()
                val category = categoryImport.first.getOrNull(categoryId ?: -1)
                        ?: fallbackCategory.also {
                    missingCategories.add(lineNumber + 1)
                }
                val interval: Int? = valueList[5].toIntOrNull()

                if (id != null && cost != null && date != null && interval != null ) {
                    importedExpenses.add(Expense(id, description, cost, date, category, interval))
                }

            } else corruptedLines.add(lineNumber + 1)
        }

        return listOf(corruptedLines, missingCategories)
    }

    private fun importCategoriesFromCSV(data: List<String>): Pair<List<Category>, List<String>> {
        val importedCategories = mutableListOf<Category>()
        var expenseData = data

        data.forEachIndexed { index, line ->
            val valueList = line.split(",")
            if (valueList.size == 6) {
                val id = valueList[0].toIntOrNull()
                val name = valueList[1]
                val icon = valueList[2].toIntOrNull()
                val position = valueList[3].toIntOrNull()
                val category: Int? = valueList[4].toIntOrNull()
                val interval: Int? = valueList[5].toIntOrNull()

                if (id != null && icon != null &&
                        position != null && category != null && interval != null) {
                    importedCategories.add(Category(id, name, icon, position, category, interval))
                }
            } else if (line == EXPENSE_HEADER) expenseData = data.subList(index, data.lastIndex)
        }
        return Pair(importedCategories, expenseData)
    }

    private fun performIncrementalImport(expenses: List<Expense>, categories: List<Category>) {
        // TODO implement function
    }

    private fun createDataString(): String {
        val data = StringBuilder().append("$HEADER_STRING\n").append(CATEGORY_HEADER)
        val expenses = db.getExpensesByDate(db.getOldestDate(), db.getNewestDate())
        val categories = db.getAllCategories()

        categories.forEach {
            data.append("${it.id},")
            data.append("${it.name},")
            data.append("${it.icon},")
            data.append("${it.position},")
            data.append("${it.budget},")
            data.append("${it.interval}\n")
        }

        data.append(EXPENSE_HEADER)

        expenses.forEach {
            data.append("${it.id},")
            data.append("${it.description},")
            data.append("${it.cost},")
            data.append("${it.date},")
            data.append("${it.category},")
            data.append("${it.interval}\n")
        }

        return data.toString()
    }
}