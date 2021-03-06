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

package com.korbi.simplebudget.ui.dialogs

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.res.TypedArray
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.korbi.simplebudget.R
import java.lang.IllegalStateException
import android.widget.GridView
import androidx.core.content.ContextCompat
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.model.Category
import com.korbi.simplebudget.utilities.CAT_INDEX
import kotlinx.android.synthetic.main.add_edit_category_dialog.*
import java.lang.ClassCastException



class AddEditCategoryDialog : DialogFragment() {

    private lateinit var categoryNameView: EditText
    private lateinit var iconGridView: GridView
    private lateinit var adapter: IconAdapter
    private val db = DBhandler.getInstance()
    private lateinit var listener: OnSaveListener
    private var prefillCategory: Category? = null

    interface OnSaveListener {
        fun onSave(category: Category, isUpdate: Boolean)
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        try {
            listener = activity as OnSaveListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$activity must implement OnSaveListener")
        }

        return activity?.let {
            val builder = AlertDialog.Builder(it)

            adapter = IconAdapter(requireContext())

            builder.setView(requireActivity().layoutInflater.inflate(R.layout.add_edit_category_dialog, null))
                    .setTitle(getString(R.string.add_edit_category_dialog_titel))
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        save()
                        dialog.dismiss()
                    }

            val dialog = builder.create()
            dialog.create()

            dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = false

            categoryNameView = dialog.add_edit_category_edit_text
            categoryNameView.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s != null) {
                        dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = !s.isEmpty()
                                                                && adapter.getSelected() != null
                    }
                }
                override fun beforeTextChanged(s: CharSequence?,
                                               start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            iconGridView = dialog.add_edit_category_icon_grid
            iconGridView.adapter = adapter

            iconGridView.setOnItemClickListener { _, _, position, _ ->
                (iconGridView.adapter as IconAdapter).setSelected(position)
                if (!categoryNameView.text.isEmpty()) {
                    dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = true
                }
            }

            prefill()

            dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }
    
    private fun save() {
        val categoryName = categoryNameView.text.toString()
        val categoryIcon = adapter.getSelected()
        if (categoryIcon != null) {

            if (prefillCategory == null) { //save new category

                Category(db.getLatestCategoryID(), categoryName, categoryIcon,
                        db.getAllCategories().size, 0, 0)

            } else { //update category

                prefillCategory?.let {
                    Category(it.id, categoryName, categoryIcon, it.position, it.budget, it.interval)
                }

            } ?.also {
                listener.onSave(it, prefillCategory != null)
            }
        }

    }

    private fun prefill() {

        arguments?.getInt(CAT_INDEX)?.let { id ->
            prefillCategory = db.getCategoryById(id)
            prefillCategory?.let {
                adapter.setSelected(it.icon)
                categoryNameView.setText(it.name)
            }
        }
    }

    inner class IconAdapter(context: Context) : BaseAdapter() {

        private val iconIDs: TypedArray = context.resources.obtainTypedArray(R.array.category_icons)
        private var selectedItem:Int? = null

        fun setSelected(position: Int) {
            selectedItem = position
            notifyDataSetChanged()
        }

        fun getSelected(): Int? {
            return selectedItem
        }

        override fun getCount(): Int {
            return iconIDs.length()
        }

        override fun getItem(position: Int): Any {
            return position
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        @SuppressLint("InflateParams")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val inflater: LayoutInflater = context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            val view = when (convertView) {
                null -> {
                    val v = inflater.inflate(R.layout.icon_grid_entry, null)
                    val iconView = v.findViewById<ImageView>(R.id.icon_grid_icon)
                    iconView.setImageResource(iconIDs.getResourceId(position, -1))
                    v
                }
                else -> {
                    val iconView = convertView.findViewById<ImageView>(R.id.icon_grid_icon)
                    iconView.setImageResource(iconIDs.getResourceId(position, -1))
                    convertView
                }
            }

            if (position != selectedItem) {
                val attrs = IntArray(1){R.attr.selectableItemBackground}
                val typedArray = requireContext().obtainStyledAttributes(attrs)
                val backgroundResource = typedArray.getResourceId(0, 0)
                typedArray.recycle()
                view.setBackgroundResource(backgroundResource)
            } else {
                view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark))
            }

            return view
        }
    }
}