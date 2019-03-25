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

package com.korbi.simplebudget.logic

import com.bignerdranch.expandablerecyclerview.model.Parent
import com.korbi.simplebudget.logic.Expense

class HistoryEntry(private val expenses: MutableList<Expense>,
                   private val dateString: String) : Parent<Expense>{

    override fun getChildList(): MutableList<Expense> {
        return expenses
    }

    override fun isInitiallyExpanded(): Boolean {
        return false
    }

    fun getDateString(): String {
        return dateString
    }
}
