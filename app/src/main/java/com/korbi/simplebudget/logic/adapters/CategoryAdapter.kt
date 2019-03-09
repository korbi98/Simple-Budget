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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.logic.Category
import kotlinx.android.synthetic.main.category_listening.view.*



class CategoryAdapter(private val categories: MutableList<Category>) :
                        RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var selectedCategory: Category? = null
    private val iconIdArray: TypedArray = SimpleBudgetApp.res.obtainTypedArray(R.array.expense_icons)

    init {
        categories.sortBy { it.position }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val categoryView = LayoutInflater.from(parent.context).inflate(R.layout.category_listening,
                parent, false)

        return ViewHolder(categoryView)
    }

    override fun onBindViewHolder(parent: ViewHolder, position: Int) {

        parent.categoryNameView.text = categories[position].name
        parent.categoryIconView.setImageResource(iconIdArray.getResourceId(categories[position].icon, -1))

        if (selectedCategory == categories[position]){
            parent.itemView.setBackgroundResource(R.color.colorPrimaryDark)
        } else {
            parent.itemView.setBackgroundResource(android.R.color.transparent)
        }
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    inner class ViewHolder(categoryView: View) : RecyclerView.ViewHolder(categoryView) {

        val categoryNameView: TextView = categoryView.category_listening_name
        val categoryIconView: ImageView = categoryView.category_listening_icon

        init {
            categoryView.setOnClickListener {
                if (selectedCategory == categories[adapterPosition]) {
                    selectedCategory = null
                    notifyDataSetChanged()
                } else {
                    selectedCategory = categories[adapterPosition]
                    notifyDataSetChanged()
                }
            }
        }
    }

    fun getSelectedCategory(): Category? {
        return selectedCategory
    }

    fun setSelectedCategory(category: Category) {
        selectedCategory = category

        notifyItemChanged(categories.indexOf(category))
    }
}