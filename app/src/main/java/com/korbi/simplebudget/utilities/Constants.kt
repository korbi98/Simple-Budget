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

package com.korbi.simplebudget.utilities

const val NON_RECURRING = -1
const val MONTHLY_ROOT = -2
const val WEEKLY_ROOT = -3

const val ALL_TIME = 4
const val YEARLY_INTERVAL = 3
const val QUARTERLY_INTERVAL = 2
const val MONTHLY_INTERVAL = 1
const val WEEKLY_INTERVAL = 0

const val CAT_INDEX = "id"
const val INCOME_INDEX = "id"

const val EXPENSE_INDEX = "prefill_index"
const val EXPENSE_DESC = "prefill_desc"
const val EXPENSE_COST = "prefill_cost"
const val EXPENSE_DATE = "prefill_date"
const val EXPENSE_CAT = "prefill_cat"

const val SET_TOTAL_BUDGET = -100
const val NO_SELECTION = 100

const val CATEGORY_TABLE = "categories"
const val EXPENSE_TABLE = "expenses"

const val COL_ID = "_id"
const val COL_COST = "cost"
const val COL_DESCRIPTION = "description"
const val COL_DATE = "date"
const val COL_INTERVAL = "interval"
const val COL_CATEGORY = "category"
const val COL_DRAWABLE = "drawable"
const val COL_POSITION = "position"
const val COL_BUDGET = "budget"
