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

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.res.TypedArray
import android.os.Bundle
import android.util.Log
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
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout
import com.korbi.simplebudget.SimpleBudgetApp
import org.threeten.bp.LocalDate
import org.threeten.bp.Year
import org.threeten.bp.format.DateTimeFormatter

const val TYPE_PREFILL = "type"
const val DATE_PREFILL = "date"
const val CATEGORY_PRESELECT = "category"
const val FROM_DATE_PRESELECT = "from_date"
const val TO_DATE_PRESELECT = "to_date"

const val TYPE_BOTH = 0
const val TYPE_EXPENSE = 1
const val TYPE_INCOME = 2

const val SELECT_ALL = 0
const val SELECT_LAST30 = 1
const val SELECT_LAST90 = 2
const val SELECT_YEAR = 3
const val SELECT_CUSTOM = 4

const val SET_FROM_DATE = 0
const val SET_TO_DATE = 1

class FilterBottomSheet :  BottomSheetDialogFragment() {

    private lateinit var listener: OnFilterFragmentListener
    private lateinit var categoryGroup: ChipGroup
    private lateinit var typeGroup: ChipGroup
    private lateinit var dateGroup: ChipGroup
    private lateinit var fromDateLayout: TextInputLayout
    private lateinit var toDateLayout: TextInputLayout
    private lateinit var fromDateInput: EditText
    private lateinit var toDateInput: EditText
    private lateinit var categoryChips: MutableList<Chip>
    private lateinit var customDateSelection: LinearLayout
    private lateinit var typeArray: Array<Int>
    private lateinit var dateArray: Array<Int>
    private var typeSelection = TYPE_BOTH
    private var dateSelection = SELECT_ALL
    private var fromDate: LocalDate = LocalDate.now()
    private var toDate: LocalDate = LocalDate.now()
    private var categorySelection = BooleanArray(DBhandler.getInstance().getAllCategories().size)

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                            savedInstanceState: Bundle?): View? {


        val rootView = inflater.inflate(R.layout.filter_bottom_sheet, container, false)

        typeArray = arrayOf(R.id.chip_both, R.id.chip_expense, R.id.chip_income)
        dateArray = arrayOf(R.id.chip_all_time, R.id.chip_last30, R.id.chip_last90,
                            R.id.chip_this_year, R.id.chip_specific_time)

        typeGroup = rootView.findViewById(R.id.filter_type_group)
        dateGroup = rootView.findViewById(R.id.filter_date_chip_group)
        categoryGroup = rootView.findViewById(R.id.filter_category_chip_group)
        categoryChips = mutableListOf()

        typeGroup.setOnCheckedChangeListener { group, checkedId ->
            typeSelection = when (checkedId) {
                R.id.chip_both -> TYPE_BOTH
                R.id.chip_expense -> TYPE_EXPENSE
                R.id.chip_income -> TYPE_INCOME
                else -> {
                    group.check(R.id.chip_both)
                    TYPE_BOTH
                }
            }
            filter()
        }

        dateGroup.setOnCheckedChangeListener { group, checkedId ->
            dateSelection = when (checkedId) {
                R.id.chip_last30 -> SELECT_LAST30
                R.id.chip_last90 -> SELECT_LAST90
                R.id.chip_this_year -> SELECT_YEAR
                R.id.chip_all_time -> SELECT_ALL
                R.id.chip_specific_time -> SELECT_CUSTOM
                else -> {
                    group.check(R.id.chip_all_time)
                    SELECT_ALL
                }
            }
            checkIfShowCustomSelection()
            filter()
        }

        fromDateLayout = rootView.findViewById(R.id.filter_date_from_input_layout)
        toDateLayout = rootView.findViewById(R.id.filter_date_to_input_layout)
        fromDateInput = rootView.findViewById(R.id.filter_date_from_input)
        toDateInput = rootView.findViewById(R.id.filter_date_to_input)

        fromDateInput.setOnClickListener {
            setDate(SET_FROM_DATE)
        }

        toDateInput.setOnClickListener {
            setDate(SET_TO_DATE)
        }

        val cancelButton = rootView.findViewById<Button>(R.id.filter_cancel_button)
        cancelButton.setOnClickListener {
            typeSelection = TYPE_BOTH
            dateSelection = SELECT_ALL
            categorySelection = BooleanArray(categoryChips.size) { true }
            filter()
            dialog?.cancel()
        }

        val filterButton = rootView.findViewById<Button>(R.id.filter_confirm_button)
        filterButton.setOnClickListener {
            dialog?.dismiss()
        }

        customDateSelection = rootView.findViewById(R.id.filter_specific_range_layout)

        setupCategoryGroup()
        prefillSelection()
        checkIfShowCustomSelection()
        updateDatePickerText()

        return rootView
    }

    private fun setupCategoryGroup(){
        val iconIdArray: TypedArray = SimpleBudgetApp.res.obtainTypedArray(R.array.category_icons)
        val db = DBhandler.getInstance()
        val categories = db.getAllCategories()
        categories.sortBy { it.position }

        for ((index, cat) in categories.withIndex()) {
            val chip = Chip(context)
            chip.isClickable = true
            chip.isCheckable = true
            chip.chipBackgroundColor = ContextCompat.getColorStateList(context!!,
                                        R.color.custom_choice_chip_selector)
            chip.text = cat.name
            chip.setChipIconTintResource(R.color.text_color_white)
            //chip.setCheckedIconResource(R.drawable.ic_done_white_24px)
            chip.setChipIconResource(iconIdArray.getResourceId(cat.icon ,-1))
            chip.setOnCheckedChangeListener { _, isChecked ->
                chip.isChipIconVisible = !isChecked
                categorySelection[index] = isChecked
                filter()
            }
            categoryGroup.addView(chip)
            categoryChips.add(chip)
        }
        iconIdArray.recycle()
    }

    private fun prefillSelection() {
        val arg = arguments
        if (arg != null) {

            typeGroup.check(typeArray[arg.getInt(TYPE_PREFILL)])
            dateGroup.check(dateArray[arg.getInt(DATE_PREFILL)])
            Log.d("test", arg.getString(FROM_DATE_PRESELECT))
            fromDate = LocalDate.parse(arg.getString(FROM_DATE_PRESELECT), dateFormatter)
            toDate = LocalDate.parse(arg.getString(TO_DATE_PRESELECT), dateFormatter)

            val catList = arg.getBooleanArray(CATEGORY_PRESELECT)

            categorySelection = if (catList != null  && !catList!!.none { !it }) {
                catList
            } else {
                BooleanArray(categoryChips.size) { false }
            }
        }
        for ((index, isSelected) in categorySelection.withIndex()) {
            categoryChips[index].isChecked = isSelected
        }
        filter()
    }

    private fun filter() {
        when {
            categorySelection.none { it } -> {
                listener.onSelectionChanged(typeSelection, dateSelection, fromDate, toDate,
                        categorySelection.map { !it }.toBooleanArray())
            }
            else -> {
                listener.onSelectionChanged(typeSelection, dateSelection, fromDate, toDate,
                        categorySelection)
            }
        }
    }

    private fun checkIfShowCustomSelection() {
        customDateSelection.visibility = when(dateGroup.checkedChipId) {
            R.id.chip_specific_time -> LinearLayout.VISIBLE
            else -> LinearLayout.GONE
        }
    }

    private fun setDate(fromOrTo: Int) {
        //LocalDate.monthValue goes from 1 to 12 while the datePicker takes values from 0 to 11
        val now = LocalDate.now()
        val year = now.year
        val month = now.monthValue - 1
        val day = now.dayOfMonth

        val datePickerDialog = DatePickerDialog(context,
                DatePickerDialog.OnDateSetListener { _, sYear, sMonth, sDay ->

                    when (fromOrTo) {
                        SET_FROM_DATE -> fromDate = LocalDate.of(sYear, sMonth + 1, sDay)
                        SET_TO_DATE -> toDate = LocalDate.of(sYear, sMonth + 1, sDay)
                    }
                    updateDatePickerText()
                    filter()

                }, year, month, day)

        datePickerDialog.show()
    }

    private fun updateDatePickerText() {

        when (LocalDate.now()) {
            fromDate -> fromDateInput.setText(R.string.today)
            fromDate.plusDays(1) -> fromDateInput.setText(R.string.yesterday)
            else -> fromDateInput.setText(dateFormatter.format(fromDate))
        }

        when (LocalDate.now()) {
            toDate -> toDateInput.setText(getString(R.string.today))
            toDate.plusDays(1) -> toDateInput.setText(R.string.yesterday)
            else -> toDateInput.setText(dateFormatter.format(toDate))
        }

        if (fromDate.isAfter(toDate)) {
            fromDateLayout.error = getString(R.string.from_after_to_error)
            toDateLayout.error = getString(R.string.to_before_from_error)
        } else {
            fromDateLayout.error = ""
            toDateLayout.error = ""
        }
    }

    fun setListener(listener: OnFilterFragmentListener) {
        this.listener = listener
    }

    interface OnFilterFragmentListener {
        fun onSelectionChanged(type: Int,
                               date: Int,
                               fromDate: LocalDate,
                               toDate: LocalDate,
                               categories: BooleanArray)
    }
}
