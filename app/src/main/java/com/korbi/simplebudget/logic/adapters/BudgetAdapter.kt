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

import android.content.res.TypedArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import kotlinx.android.synthetic.main.budget_listening.view.*

class BudgetAdapter() : RecyclerView.Adapter<BudgetAdapter.ViewHolder>() {

    private val db = DBhandler.getInstance()
    private val categories = db.getAllCategories()
    private val iconIdArray: TypedArray = SimpleBudgetApp.res.obtainTypedArray(R.array.category_icons)

    init {
        categories.sortBy { it.position }
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
        holder.categoryBudgetView.text = "200€ of 350€"
        holder.categoryProgressView.progress = 66
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    inner class ViewHolder(budgetView: View) : RecyclerView.ViewHolder(budgetView) {

        val categoryIconView: ImageView = budgetView.budget_listening_category_icon
        val categoryNameView: TextView = budgetView.budget_listening_category_name
        val categoryProgressView: ProgressBar = budgetView.budget_listening_category_progress
        val categoryBudgetView: TextView = budgetView.budget_listening_category_budget
    }
}