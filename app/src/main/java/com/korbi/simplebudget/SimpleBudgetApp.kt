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

package com.korbi.simplebudget

import android.app.Application
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import java.text.DecimalFormat

class SimpleBudgetApp : Application() {



    companion object {
        lateinit var res: Resources
        lateinit var pref: SharedPreferences
        private val decimalFormat = DecimalFormat("#0.00")

        fun createCurrencyString(amount: Int): String {
            val onLeft = pref.getBoolean(
                    res.getString(R.string.settings_key_currency_left), false)
            val noDecimal = pref.getBoolean(
                    res.getString(R.string.settings_key_currency_decimal), false)
            val amountString = when (noDecimal) {
                false -> decimalFormat.format(amount.toFloat()/100).toString()
                true -> amount.toString()
            }
            val currencySymbol = pref.getString(res.getString(R.string.settings_key_currency),
                                    res.getStringArray(R.array.currencies_symbols)[0])
            return when (onLeft) {
                false -> "$amountString $currencySymbol"
                true -> "$currencySymbol $amountString"
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        res = resources
        pref = PreferenceManager.getDefaultSharedPreferences(this)
    }

}