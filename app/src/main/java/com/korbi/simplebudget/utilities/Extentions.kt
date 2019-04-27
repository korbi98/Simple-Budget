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

import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import org.threeten.bp.LocalDate
import java.text.DecimalFormat
import kotlin.math.round

inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum = 0L
    for (element in this) sum += selector(element)
    return  sum
}

fun Long.createCurrencyString(omitDecimalIfInteger: Boolean = false,
                         omitCurrencySymbol: Boolean = false): String {

    val onLeft = SimpleBudgetApp.pref.getBoolean(
            SimpleBudgetApp.res.getString(R.string.settings_key_currency_left), false)
    val amountString = when (SimpleBudgetApp.pref.getBoolean(
            SimpleBudgetApp.res.getString(R.string.settings_key_currency_decimal), false)) {

        false -> {
            val decimalAmount = this.toFloat()/100
            val hasNoDecimal = round(decimalAmount) == decimalAmount

            if (omitDecimalIfInteger && hasNoDecimal) {
                round(decimalAmount).toInt().toString()
            } else {
                SimpleBudgetApp.decimalFormat.format(this.toDouble()/100).toString()
            }
        } true -> String.format("%,d", this)
    }

    val currencySymbol = SimpleBudgetApp.pref.getString(
            SimpleBudgetApp.res.getString(R.string.settings_key_currency),
            SimpleBudgetApp.res.getStringArray(R.array.currencies_symbols)[0])
    return when {
        omitCurrencySymbol -> amountString
        onLeft -> "$currencySymbol $amountString"
        else -> "$amountString $currencySymbol"
    }
}

fun Long.createCurrencyStringRoundToInt(): String {
    val currencyFormat = DecimalFormat("#")
    val currencySymbol = SimpleBudgetApp.pref
            .getString(SimpleBudgetApp.res.getString(R.string.settings_key_currency), "$")

    val amountString = when (SimpleBudgetApp.pref.getBoolean(
            SimpleBudgetApp.res.getString(R.string.settings_key_currency_decimal), false)) {
        true -> currencyFormat.format(this)
        false -> currencyFormat.format(this/100)
    }
    return when (SimpleBudgetApp.pref.getBoolean(
            SimpleBudgetApp.res.getString(R.string.settings_key_currency_left), false)) {
        true -> "$currencySymbol$amountString"
        false -> "$amountString$currencySymbol"
    }
}

fun Long.createCurrencyStringForEditText(): String {
    return when (SimpleBudgetApp.pref.getBoolean(
            SimpleBudgetApp.res.getString(R.string.settings_key_currency_decimal), false)) {
        true -> this.toString()
        false -> String.format("%.2f", this.toDouble() / 100)
    }
}

fun String.parseCurrencyAmount(): Long? {
    var tmp = this.replace(",", ".")

    if (!SimpleBudgetApp.pref.getBoolean(
                    SimpleBudgetApp.res.getString(R.string.settings_key_currency_decimal), false)) {

        when(tmp.split(".").getOrNull(1)?.length ?: 0) {
            0 -> tmp += "00"
            1 -> tmp += "0"
        }
    }

    return tmp.replace(".", "").toLongOrNull()
}

fun LocalDate.isBetween(start: LocalDate, end: LocalDate): Boolean {
    return this.isAfter(start.minusDays(1)) &&
            this.isBefore(end.plusDays(1))
}