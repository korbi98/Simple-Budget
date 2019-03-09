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

import android.annotation.SuppressLint
import android.content.res.TypedArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.Category
import com.korbi.simplebudget.logic.dragAndDrop.ItemTouchHelperAdapter
import com.korbi.simplebudget.logic.dragAndDrop.ItemTouchHelperViewHolder
import kotlinx.android.synthetic.main.category_manager_listening.view.*
import java.util.*

class CategoryManagerAdapter(private val categoryList: MutableList<Category>,
                             val startDragListener: OnStartDragListener) :
                            RecyclerView.Adapter<CategoryManagerAdapter.ViewHolder>(),
                            ItemTouchHelperAdapter {
    init {
        categoryList.sortBy { it.position }
    }

    interface OnStartDragListener {
        fun onStartDrag(viewHolder: ViewHolder)
    }

    private val iconIdArray: TypedArray = SimpleBudgetApp.res.obtainTypedArray(R.array.category_icons)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val categoryView = LayoutInflater.from(parent.context)
                .inflate(R.layout.category_manager_listening, parent, false)
        return ViewHolder(categoryView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.categoryNameView.text = categoryList[position].name
        val iconId = iconIdArray.getResourceId(categoryList[position].icon, -1)
        holder.categoryIconView.setImageResource(iconId)
    }

    override fun getItemCount(): Int {
        return categoryList.size
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
        Collections.swap(categoryList, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)

        for ((pos, cat) in categoryList.withIndex()) {
            DBhandler.getInstance().updatePosition(cat, pos)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ViewHolder(categoryView: View) : RecyclerView.ViewHolder(categoryView),
                                                    ItemTouchHelperViewHolder {

        val categoryNameView: TextView = categoryView.category_manager_listening_name
        val categoryIconView: ImageView = categoryView.category_manager_listening_icon
        private val dragHandle: ImageView = categoryView.category_manager_listening_drag_handle

        init {
            categoryView.setOnClickListener {  }

            dragHandle.setOnTouchListener { _, _ ->
                startDragListener.onStartDrag(this@ViewHolder)
                true
            }
        }

        override fun onItemClear() {
            itemView.setBackgroundColor(0)
        }

        override fun onItemSelected() {
            itemView.setBackgroundResource(R.color.darker_background)
        }
    }
}