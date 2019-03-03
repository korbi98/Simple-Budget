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

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.telephony.mbms.MbmsErrors
import android.util.Log
import android.util.SparseBooleanArray
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.korbi.simplebudget.R
import com.korbi.simplebudget.database.DBhandler
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.widget.FrameLayout

const val TYPE_PREFILL = "type"
const val DATE_PREFILL = "date"
const val CATEGORY_PRESELECT = "category"

class FilterBottomSheet :  BottomSheetDialogFragment() {

    private lateinit var listener: OnFilterFragmentListener
    private lateinit var categoryList: ListView
    private lateinit var typeSpinner: Spinner
    private lateinit var dateRangeSpinner: Spinner
    private var initialType = 0
    private var initialDateSelection = 3
    private var initialCategories = IntArray(DBhandler.getInstance().getAllCategories().size)


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                            savedInstanceState: Bundle?): View? {


        val rootView = inflater.inflate(R.layout.filter_bottom_sheet, container, false)

        typeSpinner = rootView.findViewById(R.id.filter_type_spinner)
        typeSpinner.adapter = ArrayAdapter.createFromResource(context!!, R.array.expense_types,
                                    R.layout.spinner_item)
                .also { adapter ->
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                }

        dateRangeSpinner = rootView.findViewById(R.id.filter_date_range_spinner)
        dateRangeSpinner.adapter = ArrayAdapter.createFromResource(context!!, R.array.date_range,
                                                                    R.layout.spinner_item)
                .also { adapter ->
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                }

        categoryList = rootView.findViewById(R.id.filter_category_list)

        val cancelButton = rootView.findViewById<Button>(R.id.filter_cancel_button)
        cancelButton.setOnClickListener {
            dialog?.cancel()
        }

        val filterButton = rootView.findViewById<Button>(R.id.filter_confirm_button)
        filterButton.setOnClickListener {
            dialog?.dismiss()
        }

        setupCategoryList()
        prefillSelection()

        return rootView
    }

    private fun setupCategoryList(){
        val db = DBhandler.getInstance()
        val categories = db.getAllCategories()
        categories.add(0, getString(R.string.all_categories))

        val adapter = ArrayAdapter(context!!, android.R.layout.simple_list_item_multiple_choice, categories)
        categoryList.adapter = adapter

        if (adapter.count > 5) {
            val item = adapter.getView(0, null, categoryList)
            item.measure(0, 0)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                (8 * item.measuredHeight))
            categoryList.layoutParams = params
        }

        categoryList.setOnItemClickListener { _, _, position, _ ->
            if (position != 0 && categoryList.checkedItemPositions[position]){
                categoryList.setItemChecked(0, false)
            }
            if (position == 0) {
                for (i in 1..categories.size) {
                    categoryList.setItemChecked(i, false)
                }
            }
        }

    }

    private fun prefillSelection() {
        val arg = arguments
        if (arg != null) {
            typeSpinner.setSelection(arg.getInt(TYPE_PREFILL))
            dateRangeSpinner.setSelection(arg.getInt(DATE_PREFILL))
            initialType = arg.getInt(TYPE_PREFILL)
            initialDateSelection = arg.getInt(DATE_PREFILL)

            val catList = arg.getIntArray(CATEGORY_PRESELECT)

            if (catList != null) {
                if (!catList!!.contentEquals(IntArray(catList.size){0}) &&
                        !catList!!.contentEquals(IntArray(catList.size){1})) {

                    initialCategories = catList
                    for ((index, values) in catList.withIndex()) {
                        categoryList.setItemChecked(index+1, values==1)
                    }

                } else {
                    categoryList.setItemChecked(0, true)
                }
            } else {
                categoryList.setItemChecked(0, true)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        bottomSheetDialog.setOnShowListener {
            val bottomSheet = bottomSheetDialog.
                    findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)

            val behavior = BottomSheetBehavior.from<FrameLayout>(bottomSheet!!)
            behavior.isHideable = false
        }
        return bottomSheetDialog
    }

    private fun getCategorySelectionArray(): IntArray{
        val selectedCategories: IntArray
        if (categoryList.checkedItemPositions[0]) {
            selectedCategories = IntArray(DBhandler.getInstance().getAllCategories().size) {1}
        } else {
            selectedCategories = IntArray(DBhandler.getInstance().getAllCategories().size)

            for (i in selectedCategories.indices) {
                selectedCategories[i] = if (categoryList.checkedItemPositions[i+1]) {
                    1
                } else {
                    0
                }
            }
        }
        return selectedCategories
    }

    override fun onStop() {
        super.onStop()
        listener.onSelectionChanged(typeSpinner.selectedItemPosition,
                                    dateRangeSpinner.selectedItemPosition,
                                    getCategorySelectionArray())
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        typeSpinner.setSelection(initialType)
        dateRangeSpinner.setSelection(initialDateSelection)

        if (!initialCategories.contentEquals(IntArray(initialCategories.size){0})) {
            categoryList.setItemChecked(0, false)
            for ((index, value) in initialCategories.withIndex()) {
                categoryList.setItemChecked(index + 1, value == 1)
            }
        } else {
            categoryList.setItemChecked(0, true)
        }

    }

    fun setListener(listener: OnFilterFragmentListener) {
        this.listener = listener
    }

    interface OnFilterFragmentListener {
        fun onSelectionChanged(type: Int, date: Int, categories: IntArray)
    }
}
