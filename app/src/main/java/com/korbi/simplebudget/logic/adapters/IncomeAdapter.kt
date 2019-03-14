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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.logic.RecurrentEntry
import kotlinx.android.synthetic.main.income_manager_listening.view.*

class IncomeAdapter(private val incomeList: MutableList<RecurrentEntry>) : RecyclerView.Adapter<IncomeAdapter.ViewHolder>() {

    private val iconIdArray: TypedArray = SimpleBudgetApp.res.obtainTypedArray(R.array.category_icons)

    override fun getItemCount(): Int {
        return incomeList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val incomeView = LayoutInflater.from(parent.context)
                .inflate(R.layout.income_manager_listening, parent, false)
        return ViewHolder(incomeView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.incomeNameView.text = incomeList[position].name
        val iconId = iconIdArray.getResourceId(incomeList[position].category.icon, -1)
        holder.incomeIconView.setImageResource(iconId)
        holder.incomeAmountView.text = "200 €"
        holder.incomeIntervalView.text = "On first day of every month"
    }



    inner class ViewHolder(incomeView: View) : RecyclerView.ViewHolder(incomeView) {

        val incomeIconView: ImageView = incomeView.income_manager_listening_icon
        val incomeNameView: TextView = incomeView.income_manager_listening_description
        val incomeIntervalView: TextView = incomeView.income_manager_listening_date
        val incomeAmountView: TextView = incomeView.income_manager_listening_amount


    }
}