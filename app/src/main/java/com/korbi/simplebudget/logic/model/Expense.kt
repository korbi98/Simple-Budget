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

package com.korbi.simplebudget.logic.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.korbi.simplebudget.utilities.*
import org.threeten.bp.LocalDate

@Entity(tableName = EXPENSE_TABLE)
data class Expense(@PrimaryKey @ColumnInfo(name = COL_ID) var id: Int,
                   @ColumnInfo(name = COL_DESCRIPTION) var description: String,
                   @ColumnInfo(name = COL_COST) var cost: Int,
                   @ColumnInfo(name = COL_DATE) var date: LocalDate,
                   @ColumnInfo(name = COL_CATEGORY) var category: Category,
                   @ColumnInfo(name = COL_INTERVAL) var interval: Int) {


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