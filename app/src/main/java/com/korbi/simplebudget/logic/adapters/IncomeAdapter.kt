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
import android.text.Layout
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.Category
import com.korbi.simplebudget.logic.Expense
import com.korbi.simplebudget.logic.MONTHLY_ROOT
import com.korbi.simplebudget.logic.WEEKLY_ROOT
import com.korbi.simplebudget.ui.dialogs.INCOME_INDEX
import kotlinx.android.synthetic.main.income_manager_listening.view.*
import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.util.*

class IncomeAdapter(private val incomeList: MutableList<Expense>,
                    val editListener: OnEditListener) :
        RecyclerView.Adapter<IncomeAdapter.ViewHolder>() {

    private val iconIdArray: TypedArray = SimpleBudgetApp.res
            .obtainTypedArray(R.array.category_icons)

    private val weekDayArray = SimpleBudgetApp.res.getStringArray(R.array.weekdays)
    private val ordinalNumbers = SimpleBudgetApp.res.getStringArray(R.array.ordinals)

    interface OnEditListener {
        fun onEdit(income: Expense)
        fun onDelete(income: Expense)
    }

    override fun getItemCount(): Int {
        return incomeList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val incomeView = LayoutInflater.from(parent.context)
                .inflate(R.layout.income_manager_listening, parent, false)
        return ViewHolder(incomeView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.incomeNameView.text = when {
            !incomeList[position].description.isBlank() -> {
                incomeList[position].description
            }
            else -> incomeList[position].category.name
        }
        val iconId = iconIdArray.getResourceId(incomeList[position].category.icon, -1)
        holder.incomeIconView.setImageResource(iconId)
        holder.incomeAmountView.text = SimpleBudgetApp.createCurrencyString(incomeList[position].cost)
        holder.incomeIntervalView.text = when (incomeList[position].interval) {
            WEEKLY_ROOT -> {
                holder.itemView.context.getString(R.string.every) +
                        " ${weekDayArray[incomeList[position].date.dayOfWeek.value - 1]}"
            }
            MONTHLY_ROOT -> {
                holder.itemView.context.getString(R.string.every) +
                        " ${ordinalNumbers[incomeList[position].date.dayOfMonth - 1]} " +
                        holder.itemView.context.getString(R.string.of_month)
            }
            else -> "ERROR, THIS SHOULD NOT BE HERE"
        }

        if (incomeList[position].cost < 0) {
            holder.incomeAmountView.setTextColor(ContextCompat.getColor(holder.itemView.context,
                    R.color.expenseColor))
        } else {
            holder.incomeAmountView.setTextColor(ContextCompat.getColor(holder.itemView.context,
                    R.color.incomeColor))
        }
    }

    inner class ViewHolder(incomeView: View) : RecyclerView.ViewHolder(incomeView) {

        val incomeIconView: ImageView = incomeView.income_manager_listening_icon
        val incomeNameView: TextView = incomeView.income_manager_listening_description
        val incomeIntervalView: TextView = incomeView.income_manager_listening_date
        val incomeAmountView: TextView = incomeView.income_manager_listening_amount

        init {
            itemView.setOnCreateContextMenuListener { menu, _, _ ->
                val edit = menu.add(Menu.NONE, 1, 1, itemView.context.getString(R.string.edit))
                val delete = menu.add(Menu.NONE, 2, 2, itemView.context.getString(R.string.delete))
                edit.setOnMenuItemClickListener {

                    editListener.onEdit(incomeList[adapterPosition])

                    true
                }
                delete.setOnMenuItemClickListener {

                    editListener.onDelete(incomeList[adapterPosition])
                    true
                }
            }
        }
    }
}