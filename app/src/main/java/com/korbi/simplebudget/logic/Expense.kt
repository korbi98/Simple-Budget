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

import org.threeten.bp.LocalDate
import java.util.Date

const val NON_RECURRING = -1
const val MONTHLY_ROOT = -2
const val WEEKLY_ROOT = -3

data class Expense(var id: Int,
                   var description: String,
                   var cost: Int,
                   var date: LocalDate,
                   var category: Category,
                   var interval: Int) {


    override fun equals(other: Any?): Boolean {
        other as Expense
        return (this.category == other.category && this.cost == other.cost &&
                this.description == other.description && this.date == other.date &&
                this.interval == other.interval)
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + description.hashCode()
        result = 31 * result + cost
        result = 31 * result + date.hashCode()
        result = 31 * result + category.hashCode()
        result = 31 * result + interval
        return result
    }
}