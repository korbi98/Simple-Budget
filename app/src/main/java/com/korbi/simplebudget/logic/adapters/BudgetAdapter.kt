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

package com.korbi.simplebudget.logic.adapters

import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.Category
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.ui.fragments.*
import kotlinx.android.synthetic.main.budget_listening.view.*
import java.text.DecimalFormat

class BudgetAdapter(private var expenses: MutableList<Expense>,
                    private var interval: Int,
                    private val listener: OnLongItemClickListener) :
                                        RecyclerView.Adapter<BudgetAdapter.ViewHolder>() {

    private val db = DBhandler.getInstance()
    private var categories = db.getAllCategories()
    private val iconIdArray: TypedArray = SimpleBudgetApp.res.obtainTypedArray(R.array.category_icons)

    interface OnLongItemClickListener {
        fun onLongClick(category: Category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val budgetView = LayoutInflater.from(parent.context)
                .inflate(R.layout.budget_listening, parent, false)
        return ViewHolder(budgetView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.categoryNameView.text = categories[position].name
        val iconId = iconIdArray.getResourceId(categories[position].icon, -1)
        holder.categoryIconView.setImageResource(iconId)
        holder.categoryBudgetView.text = getBudgetText(categories[position])
        holder.categoryProgressView.progress = getBudgetProgress(categories[position])
        if (getBudgetProgress(categories[position]) > 100) {
            holder.categoryBudgetView.setTextColor(ContextCompat.getColor(holder.itemView.context,
                    R.color.expenseColor))
            holder.categoryProgressView.progressTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.context, R.color.expenseColor))
        } else {
            holder.categoryBudgetView.setTextColor(ContextCompat.getColor(holder.itemView.context,
                    R.color.text_color_white_secondary))
            holder.categoryProgressView.progressTintList = ColorStateList.valueOf(
                    ContextCompat.getColor(holder.itemView.context, R.color.colorPrimary))
        }
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    private fun getBudgetProgress(category: Category): Int {

        val categoryTotalSum = getCategoryExpenses(category)
        val budget = getIntervalBudget(category, interval)
        val maxCategory = categories.maxBy { getCategoryExpenses(it) }
        val maxAmount = if (maxCategory != null) {
            getCategoryExpenses(maxCategory)
        } else {
            0
        }

        return when {
            interval == ALL_TIME -> {
                if (maxAmount != 0) {
                    (categoryTotalSum.toFloat() / maxAmount.toFloat() * 100).toInt()
                } else {
                    0
                }
            }
            categoryTotalSum > 0 && budget != 0 -> {
                ((categoryTotalSum.toFloat() / budget.toFloat()) * 100).toInt()
            }
            else -> 0
        }
    }

    private fun getBudgetText(category: Category): String {

        val decimalFormat = DecimalFormat(SimpleBudgetApp.res.getString(R.string.number_format))

        val onLeft = SimpleBudgetApp.pref.getBoolean(
                SimpleBudgetApp.res.getString(R.string.settings_key_currency_left), false)
        val noDecimal = SimpleBudgetApp.pref.getBoolean(
                SimpleBudgetApp.res.getString(R.string.settings_key_currency_decimal), false)

        val budget = getIntervalBudget(category, interval)
        val catExpenses = when (noDecimal) {
            true -> getCategoryExpenses(category).toString()
            false -> decimalFormat.format(getCategoryExpenses(category).toFloat()/100)
        }

        val currencySymbol = SimpleBudgetApp.pref.getString(
                SimpleBudgetApp.res.getString(R.string.settings_key_currency),
                SimpleBudgetApp.res.getStringArray(R.array.currencies_symbols)[0])

        return when {
            interval == ALL_TIME -> {
                SimpleBudgetApp.createCurrencyString(getCategoryExpenses(category))
            }
            budget != 0 -> {
                val budgetStr = when (noDecimal) {
                    true -> budget.toString()
                    false -> decimalFormat.format(budget.toFloat()/100)
                }
                when (onLeft) {
                    true -> "$currencySymbol $catExpenses / $budgetStr"
                    false -> "$catExpenses / $budgetStr $currencySymbol"
                }
            }
            else -> {
                when (onLeft) {
                    true -> "$currencySymbol $catExpenses / -"
                    false -> "$catExpenses / - $currencySymbol"
                }
            }
        }
    }

    fun setInterval(interval: Int) {
        this.interval = interval
    }

    fun setExpenses(expenses: MutableList<Expense>) {
        this.expenses = expenses
    }

    private fun isCategoryEmpty(category: Category): Boolean {
        return expenses.none { it.category == category && it.cost < 0}
    }

    private fun getCategoryExpenses(category: Category): Int {
        val categoryExpenses = expenses.filter { it.category == category && it.cost < 0}
        return -categoryExpenses.sumBy { it.cost }
    }

    fun updateCategories() {
        categories = db.getAllCategories().filter { !isCategoryEmpty(it) } .toMutableList()

        categories.sortWith(compareBy({ -getBudgetProgress(it) }, { -getCategoryExpenses(it) }))
        notifyDataSetChanged()
    }

    private fun getIntervalBudget(category: Category, interval: Int): Int {

        return when (interval) {
            ALL_TIME -> 0
            YEARLY_INTERVAL -> {
                when (category.interval) {
                    WEEKLY_INTERVAL -> category.budget * 52
                    else -> category.budget * 12
                }
            }
            QUARTERLY_INTERVAL -> {
                when (category.interval) {
                    WEEKLY_INTERVAL -> category.budget * 13
                    else -> category.budget * 4
                }
            }
            MONTHLY_INTERVAL -> {
                when (category.interval) {
                    WEEKLY_INTERVAL -> category.budget * 4
                    else -> category.budget
                }
            }
            else -> {
                when (category.interval) {
                    WEEKLY_INTERVAL -> category.budget
                    else -> {
                        (category.budget / 4.33f).toInt()
                    }
                }
            }
        }
    }

    inner class ViewHolder(budgetView: View) : RecyclerView.ViewHolder(budgetView) {

        val categoryIconView: ImageView = budgetView.budget_listening_category_icon
        val categoryNameView: TextView = budgetView.budget_listening_category_name
        val categoryProgressView: ProgressBar = budgetView.budget_listening_category_progress
        val categoryBudgetView: TextView = budgetView.budget_listening_category_budget

        init {
            budgetView.setOnLongClickListener {
                listener.onLongClick(categories[adapterPosition])
                true
            }
        }
    }
}