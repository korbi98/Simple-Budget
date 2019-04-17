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
import com.korbi.simplebudget.SimpleBudgetApp.Companion.decimalFormat
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.*
import com.korbi.simplebudget.ui.fragments.*
import com.korbi.simplebudget.utilities.MONTHLY_INTERVAL
import kotlinx.android.synthetic.main.budget_listening.view.*
import java.text.DecimalFormat

class BudgetAdapter(private val listener: OnLongItemClickListener) :
                                        RecyclerView.Adapter<BudgetAdapter.ViewHolder>() {

    var expenses = mutableListOf<Expense>()
    var interval = MONTHLY_INTERVAL

    private val db = DBhandler.getInstance()
    private var categories = mutableListOf<Category>()
    private val iconIdArray: TypedArray = SimpleBudgetApp.res.obtainTypedArray(R.array.category_icons)
    private val budgetHelper = BudgetHelper()

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
        holder.categoryBudgetView.text = budgetHelper.getBudgetText(categories[position])
        holder.categoryProgressView.progress = budgetHelper.getBudgetProgress(categories[position])


        if (budgetHelper.getBudgetProgress(categories[position]) > 100) {
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

    private fun isCategoryEmpty(category: Category): Boolean {
        return expenses.none { it.category == category && it.cost < 0}
    }

    fun updateCategories() {

        categories = db.getAllCategories().filter { !isCategoryEmpty(it) } .toMutableList()

        budgetHelper.categories = categories
        budgetHelper.expenses = expenses
        budgetHelper.interval = interval

        categories.sortWith(compareBy({ -budgetHelper.getBudgetProgress(it) }, { -budgetHelper.getCategoryExpenses(it) }))

        notifyDataSetChanged()
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