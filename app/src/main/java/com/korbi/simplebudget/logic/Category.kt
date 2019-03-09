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


data class Category(var id: Int,
                    var name: String,
                    var icon: Int,
                    var position: Int) {

    override fun equals(other: Any?): Boolean {
        other as Category
        return (this.id == other.id && this.position == other.position &&
                this.icon == other.icon && this.name == other.name)
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        result = 31 * result + icon
        result = 31 * result + position
        return result
    }
}