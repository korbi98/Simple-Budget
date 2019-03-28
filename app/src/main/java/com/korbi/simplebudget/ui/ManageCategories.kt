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
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.Category
import com.korbi.simplebudget.logic.adapters.CategoryAdapter
import com.korbi.simplebudget.logic.dragAndDrop.ItemTouchHelperCallback
import com.korbi.simplebudget.ui.dialogs.AddEditCategoryDialog
import com.korbi.simplebudget.ui.dialogs.CAT_INDEX
import kotlinx.android.synthetic.main.activity_manage_categories.*
import kotlinx.android.synthetic.main.category_manager_migrate_category.*

class ManageCategories : AppCompatActivity(), AddEditCategoryDialog.OnSaveListener,
                                                CategoryAdapter.OnEditListener,
                                                CategoryAdapter.OnStartDragListener {

    private lateinit var categoryRecycler: RecyclerView
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private val db = DBhandler.getInstance()
    private lateinit var categoryList: MutableList<Category>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_categories)
        setTitle(R.string.manage_categories_titel)

        categoryList = db.getAllCategories()
        categoryAdapter = CategoryAdapter(categoryList, this, this)

        categoryRecycler = category_recycler.apply {
            layoutManager = LinearLayoutManager(applicationContext,
                    RecyclerView.VERTICAL, false)
            setHasFixedSize(true)
            adapter = categoryAdapter
        }

        val callback = ItemTouchHelperCallback(categoryAdapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(categoryRecycler)
        registerForContextMenu(categoryRecycler)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onPause() {
        super.onPause()
        categoryAdapter.updatePositions()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.category_and_income_manager_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.menu_category_and_income_manager_add -> {
                val dialog = AddEditCategoryDialog()
                dialog.show(supportFragmentManager, "addEditCategoryDialog")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSave(category: Category, isUpdate: Boolean) {
        if (isUpdate) {
            db.updateCategory(category)
            categoryList[category.position] = category
            categoryAdapter.notifyItemChanged(categoryList.indexOf(category))
        } else {
            db.addCategory(category)
            categoryList.add(category)
            categoryAdapter.notifyItemInserted(category.position)
        }
    }

    override fun onEdit(category: Category) {
        AddEditCategoryDialog().run {
            arguments = Bundle().apply { putInt(CAT_INDEX, category.id) }
            show(supportFragmentManager, "addEditCategoryDialog")
        }
    }

    override fun onDelete(category: Category) {

        var categorySpinner: Spinner? = null

        val dialog = AlertDialog.Builder(this).run {
            setTitle(getString(R.string.delete_category))
            setMessage(getString(R.string.migrate_expenses_message))
            setPositiveButton(getString(R.string.ok)) { dialog, _ ->

                categorySpinner?.let {
                    deleteCategory(category, it.selectedItemPosition)
                }

                dialog.dismiss()
            }
            setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            setView(R.layout.category_manager_migrate_category)
        } .create()
        dialog.create()

        val newCatList = categoryList
        newCatList.remove(category)

        val categoryNameList = newCatList.map { it.name }.toMutableList()
        categoryNameList.add(0, getString(R.string.none))

        categorySpinner = dialog.category_manager_migration_spinner
        categorySpinner?.adapter = ArrayAdapter<String>(this,
                            android.R.layout.simple_spinner_dropdown_item, categoryNameList)

        dialog.show()
    }

    override fun onStartDrag(viewHolder: CategoryAdapter.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    private fun deleteCategory(category: Category, migrationIndex: Int) {

        val newCatList = categoryList
        newCatList.remove(category)
        if (migrationIndex != 0) {
            val newCategory = newCatList[migrationIndex - 1]
            db.migrateExpenses(category, newCategory)
        } else {
            db.deleteExpensesByCategory(category)
        }

        db.deleteCategory(category)
        categoryList.remove(category)
        categoryAdapter.notifyItemRemoved(category.position)
        categoryAdapter.updatePositions()
    }
}
