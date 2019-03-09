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

package com.korbi.simplebudget.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.SearchView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.adapters.CategoryAdapter
import com.korbi.simplebudget.logic.adapters.CategoryManagerAdapter
import com.korbi.simplebudget.logic.adapters.HistoryAdapter
import com.korbi.simplebudget.logic.dragAndDrop.ItemTouchHelperCallback

class ManageCategories : AppCompatActivity(),  CategoryManagerAdapter.OnStartDragListener {

    private lateinit var categoryRecycler: RecyclerView
    private lateinit var categoryAdapter: CategoryManagerAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private val db = DBhandler.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_categories)
        setTitle(R.string.manage_categories_titel)

        categoryRecycler = findViewById(R.id.category_recycler)
        categoryRecycler.setHasFixedSize(true)
        categoryRecycler.layoutManager = LinearLayoutManager(applicationContext,
                RecyclerView.VERTICAL, false)
        categoryAdapter = CategoryManagerAdapter(db.getAllCategories(), this)
        categoryRecycler.adapter = categoryAdapter

        val callback = ItemTouchHelperCallback(categoryAdapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(categoryRecycler)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onStartDrag(viewHolder: CategoryManagerAdapter.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.category_manager_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.menu_category_manager_add -> {
                val dialog = AddEditCagegoryDialog()
                dialog.show(supportFragmentManager, "addEditCategoryDialog")
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


}
