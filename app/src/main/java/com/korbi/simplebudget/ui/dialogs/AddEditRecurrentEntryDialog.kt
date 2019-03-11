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

package com.korbi.simplebudget.ui.dialogs

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.korbi.simplebudget.R

class AddEditRecurrentEntryDialog : DialogFragment(){

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let {
            val builder = AlertDialog.Builder(it)

            builder.setView(requireActivity().layoutInflater.inflate(R.layout.recurrent_entry_dialog, null))
                    .setTitle(getString(R.string.add_recurrent_entry_title))
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }
                    .setPositiveButton(R.string.ok) { dialog, _ ->
                        save()
                        dialog.dismiss()
                    }

            val dialog = builder.create()
            dialog.create()

            dialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = false



            dialog
        } ?: throw IllegalStateException("Activity cannot be null")

    }

    private fun save() {

    }
}