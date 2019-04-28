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

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.model.Category
import com.korbi.simplebudget.logic.model.Expense
import com.korbi.simplebudget.utilities.MONTHLY_ROOT
import com.korbi.simplebudget.utilities.NON_RECURRING
import com.korbi.simplebudget.utilities.WEEKLY_INTERVAL
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.io.*
import java.text.ParseException

const val HEADER_STRING = "### DO NOT MODIFY THIS FILE. Import file from settings in Simple Budget app. ###"
const val CATEGORY_HEADER = "### categories ###"
const val EXPENSE_HEADER = "### expenses ###"

object ImportExportHelper {

    private var expensesWithFallbackCategory = 0
    private var lineOffset = 3
    private val corruptedLines = mutableListOf<Int>()

    private val db = DBhandler.getInstance()

    private val fallbackCategory = Category(db.getLatestCategoryID(),
            SimpleBudgetApp.res.getString(R.string.fallback_category_name),
            45, db.getAllCategories().size, 0, 0)

    fun createFileName(): String {
        val dateString = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        return "SimpleBudget_export_$dateString.csv"
    }

    fun writeCSV(uri: Uri, context: Context) {

        try {

            context.contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { file ->
                    file.write(createDataString().toByteArray())
                }
            }
            Toast.makeText(context, context.getString(R.string.export_successful),
                    Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Toast.makeText(context, context.getString(R.string.export_unsuccessful),
                    Toast.LENGTH_LONG).show()
        } catch (e: FileNotFoundException) {
            Toast.makeText(context, context.getString(R.string.export_unsuccessful),
                    Toast.LENGTH_LONG).show()
        }
    }

    fun readCSV(uri: Uri, context: Context) {

        expensesWithFallbackCategory = 0
        corruptedLines.clear()

        val data = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readLines()
            }
        }

        val result = data?.let {

            if (it.getOrNull(0) == HEADER_STRING && it.getOrNull(1) == CATEGORY_HEADER
                    && it.size > 2) {

                    Pair(true, it.run { importExpensesFromCSV(subList(2, size)) })
            }
            else Pair(false, buildReport(false)) // invalid file
        } ?: Pair(false, buildReport(false))

        AlertDialog.Builder(context).apply {
            setTitle(when(result.first) {
                true -> context.getString(R.string.import_report)
                false -> context.getString(R.string.invalid_file)
            })
            setMessage(result.second)
            setPositiveButton(R.string.ok) {dialog, _ ->
                dialog.dismiss()
            }
            show()
        }
    }

    private fun importExpensesFromCSV(data: List<String>): String {
        val importedExpenses = mutableListOf<Expense>()
        val categoryImport = importCategoriesFromCSV(data)
        var corruptedCategories = categoryImport.third
        var corruptedExpenses = 0

        categoryImport.second.forEachIndexed { lineNumber, line ->

            val valueList = line.split(",")

            if (valueList.size == 6) {

                val id = valueList[0].toIntOrNull()
                val description = valueList[1]
                val cost = valueList[2].toLongOrNull()
                val date: LocalDate? = try {
                    LocalDate.parse(valueList[3])
                } catch (e: ParseException) {
                    null
                }
                val categoryId = valueList[4].toIntOrNull() ?: -1
                val category = categoryImport.first.find { it.id == categoryId}
                        ?: fallbackCategory.also {
                            corruptedLines.add(lineNumber + lineOffset)
                            expensesWithFallbackCategory++
                            corruptedCategories++
                }
                val interval = valueList[5].toIntOrNull()

                if (id != null && cost != null && date != null && interval != null ) {
                    importedExpenses.add(Expense(id, description, cost, date, category, interval))
                } else {
                    corruptedExpenses++
                    corruptedLines.add(lineNumber + lineOffset)
                }

            } else {
                corruptedExpenses++
                corruptedLines.add(lineNumber + lineOffset)
            }
        }

        return with(performIncrementalImport(importedExpenses, categoryImport.first)) {
            buildReport(
                    success = true,
                    importedExpenses = this[0],
                    omittedExpenses = this[1],
                    importedCategories = this[2],
                    omittedCategories = this[3],
                    corruptedExpenses = corruptedExpenses,
                    corruptedCategories = corruptedCategories,
                    corruptedLines = corruptedLines,
                    expensesWithFallback = expensesWithFallbackCategory)
        }
    }

    private fun importCategoriesFromCSV(data: List<String>):
            Triple<List<Category>, List<String>, Int> {
        val importedCategories = mutableListOf<Category>()
        var expenseData = data
        var corruptedCategories = 0

        for ((index, line) in data.withIndex()) {
            val valueList = line.split(",")
            if (valueList.size == 6) {
                val id = valueList[0].toIntOrNull()
                val name = valueList[1]
                val icon = valueList[2].toIntOrNull()
                val position = valueList[3].toIntOrNull()
                val budget = valueList[4].toLongOrNull()
                val interval = valueList[5].toIntOrNull()

                if (id != null && icon != null &&
                        position != null && budget != null && interval != null) {
                    importedCategories.add(Category(id, name, icon, position, budget, interval))
                } else {
                    corruptedLines.add(index + lineOffset)
                    corruptedCategories++
                }

            } else if (line == EXPENSE_HEADER) {
                lineOffset += index
                expenseData = data.subList(index + 1, data.size)
                break
            } else {
                corruptedCategories++
                corruptedLines.add(index + lineOffset)
            }
        }
        return Triple(importedCategories, expenseData, corruptedCategories)
    }

    private fun performIncrementalImport(expenses: List<Expense>, categories: List<Category>): List<Int> {
        val existingExpenses = db.getExpensesByDate(db.getOldestDate(), db.getNewestDate())
        val existingCategories = db.getAllCategories()

        var importedExpenses = 0
        var omittedExpenses = 0
        var importedCategories = 0
        var omittedCategories = 0

        expenses.forEach { exp ->

            if (existingExpenses.none { it.isDuplicate(exp) }) {

                if (exp.interval != MONTHLY_ROOT && exp.interval != WEEKLY_INTERVAL &&
                        exp.interval != NON_RECURRING) {
                    val parent = expenses.find {
                        (it.interval == MONTHLY_ROOT || it.interval == WEEKLY_INTERVAL) &&
                                it.description == exp.description && it.cost == exp.cost &&
                                it.category == exp.category }

                    exp.interval = parent?.id ?: 0
                }

                if (categories.none { it.isDuplicate(exp.category) }) {
                    exp.category = fallbackCategory
                    expensesWithFallbackCategory++
                }
                db.addExpense(exp)
                importedExpenses++
            } else omittedExpenses++
        }

        categories.forEach { cat ->
            if (existingCategories.none { it.isDuplicate(cat) }) {
                db.addCategory(cat)
                importedCategories++
            } else {
                existingCategories.find { it.isDuplicate(cat) }?.let {
                    it.budget = cat.budget
                    db.updateCategory(it)
                }
                omittedCategories++
            }
        }

        return listOf(importedExpenses, omittedExpenses, importedCategories, omittedCategories)
    }

    private fun createDataString(): String {
        val data = StringBuilder().append("$HEADER_STRING\n").append("$CATEGORY_HEADER\n")
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

        data.append("$EXPENSE_HEADER\n")

        expenses.forEach {
            data.append("${it.id},")
            data.append("${it.description},")
            data.append("${it.cost},")
            data.append("${it.date},")
            data.append("${it.category.id},")
            data.append("${it.interval}\n")
        }

        return data.toString()
    }

    private fun buildReport(success: Boolean,
                            importedExpenses:Int = 0,
                            omittedExpenses: Int = 0,
                            corruptedExpenses: Int = 0,
                            expensesWithFallback: Int = 0,
                            importedCategories: Int = 0,
                            omittedCategories: Int = 0,
                            corruptedCategories: Int = 0,
                            corruptedLines: List<Int> = listOf()): String {

        return if (success) {
            if (corruptedExpenses == 0 && corruptedCategories == 0 &&
                    corruptedLines.isEmpty() && expensesWithFallback == 0) {

                SimpleBudgetApp.res.getString(
                        R.string.import_successful,
                        importedExpenses,
                        omittedExpenses,
                        importedCategories,
                        omittedCategories)

            } else SimpleBudgetApp.res.getString(
                    R.string.import_corrupted,
                    importedExpenses,
                    omittedExpenses,
                    importedCategories,
                    omittedCategories,
                    corruptedExpenses,
                    corruptedCategories) +

                    SimpleBudgetApp.res.getString(R.string.corrupted_lines) +
                    StringBuilder().apply {
                        corruptedLines.forEach { append("$it, ") }
                    } .toString()

        } else SimpleBudgetApp.res.getString(R.string.invalid_file_message)
    }
}