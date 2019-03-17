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

class CurrencyTextWatcher(private val inputView: EditText,
                          private val inputLayout: TextInputLayout,
                          private val separator: String,
                          private val forbiddenSeparator: String,
                          private val dialog: AlertDialog?,
                          private val isNegativeAllowed: Boolean) : TextWatcher {

    override fun afterTextChanged(s: Editable?) {

        if (!s.isNullOrBlank()) inputLayout.error = ""
        if (dialog != null) {
            val c = s.toString()
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    !(s.isNullOrBlank() || c == "." || c == "," || c == "-" || c == "-,"
                            || c == "-.")
        }

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
            if (c == forbiddenSeparator || (position != 0 && c == "-") ||
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