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

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputLayout
import java.lang.StringBuilder
import kotlin.math.round

class CurrencyTextWatcher(private val inputView: EditText,
                          private val inputLayout: TextInputLayout,
                          private val separator: String,
                          private val isZeroAllowed: Boolean = false,
                          private val isNegativeAllowed: Boolean = true,
                          private val enableOkIfEmpty: Boolean = false,
                          private val dialog: AlertDialog? = null) : TextWatcher {

    override fun afterTextChanged(s: Editable?) {

        val forbiddenSeparator = when (separator) {
            "," -> "."
            else -> ","
        }

        if (!s.isNullOrBlank()) inputLayout.error = ""

        // enable or disable ok button for dialog
        if (dialog != null) {
            val c = s.toString().replace(",", ".")

            with(dialog.getButton(AlertDialog.BUTTON_POSITIVE)) {
                isEnabled = c.toFloatOrNull() != null

                if (!isZeroAllowed) {
                    if (c.toFloatOrNull() == null || round(c.toFloat()*100).toInt() == 0)
                        isEnabled = false
                }

                if (enableOkIfEmpty) {
                    if (s.isNullOrBlank()) isEnabled = true
                }
            }
        }

        // handle input of valid currency amount
        var hasSeparator = false
        var separatorPosition: Int? = null
        val originalString = s.toString()
        val newStringBuilder = StringBuilder()

        for ((position, char) in originalString.withIndex()) {

            var c = char.toString()
            if (c == separator) {
                if (!hasSeparator) {
                    hasSeparator = true
                    separatorPosition = position
                } else if (hasSeparator) c = ""
            }

            if (!isNegativeAllowed && c == "-") {
                c = ""
            }

            if (position == 1 && originalString[0].toString() == "0" &&
                    !"., ".contains(c)) {
                newStringBuilder.append(separator)
            }

            if (position == 2 && originalString.contains("-0") &&
                    !"., ".contains(c)) {
                newStringBuilder.append(separator)
            }

            if (c == forbiddenSeparator) {
                c = separator
            }

            if ((position != 0 && c == "-") ||
                    (position > 10) || (separatorPosition != null
                            && position > (separatorPosition + 2))) {
                c = ""
            }

            newStringBuilder.append(c)
        }

        val newString = newStringBuilder.toString()

        if (newString != originalString) {
            inputView.setText(newString)
            inputView.setSelection(inputView.text.length)
        }
    }
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
}