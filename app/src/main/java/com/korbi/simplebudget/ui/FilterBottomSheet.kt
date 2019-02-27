package com.korbi.simplebudget.ui

import android.app.Dialog
import android.os.Bundle
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

            val catList = arg.getIntArray(CATEGORY_PRESELECT)

            if (catList != null) {
                if (!catList!!.contentEquals(IntArray(catList.size){0}) &&
                        !catList!!.contentEquals(IntArray(catList.size){1})) {

                    for ((i, j) in catList.withIndex()) {
                        categoryList.setItemChecked(i+1, j==1)
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

    fun setListener(listener: OnFilterFragmentListener) {
        this.listener = listener
    }

    interface OnFilterFragmentListener {
        fun onSelectionChanged(type: Int, date: Int, categories: IntArray)
    }
}
