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
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.preference.PreferenceManager
import com.jakewharton.threetenabp.AndroidThreeTen
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.logic.HistoryHelper
import com.korbi.simplebudget.logic.model.Expense
import com.korbi.simplebudget.utilities.MONTHLY_INTERVAL
import com.korbi.simplebudget.utilities.MONTHLY_ROOT
import com.korbi.simplebudget.utilities.WEEKLY_ROOT
import com.korbi.simplebudget.widget.SimpleBudgetWidget
import org.threeten.bp.LocalDate
import java.text.DecimalFormat

class SimpleBudgetApp : Application() {

    companion object {
        var selectedInterval = 0
        var selectedIntervalType = MONTHLY_INTERVAL

        lateinit var res: Resources
        lateinit var pref: SharedPreferences
        lateinit var decimalFormat: DecimalFormat

        fun handleRecurringEntries() {

            val db = DBhandler.getInstance()
            val recurringEntries = db.getRecurringExpenses()
            val expenses = db.getExpensesByDate(db.getOldestDate(), db.getNewestDate())

            for (recurring in recurringEntries) {
                var date = recurring.date
                val maxDate = when (recurring.interval) {
                    WEEKLY_ROOT -> {
                        LocalDate.now().minusWeeks(1).plusDays(1)
                    }
                    MONTHLY_ROOT -> {
                        LocalDate.now().minusMonths(1).plusDays(1)
                    }
                    else -> LocalDate.now()
                }

                while (date.isBefore( maxDate )) {
                    when (recurring.interval) {
                        WEEKLY_ROOT -> date = date.plusWeeks(1)
                        MONTHLY_ROOT -> date = date.plusMonths(1)
                    }
                    if (expenses.none { it.interval == recurring.id && it.date == date}) {
                        val newRecurringEntry = Expense(db.getLatestCategoryID(),
                                recurring.description,
                                recurring.cost,
                                date,
                                recurring.category,
                                recurring.id)
                        db.addExpense(newRecurringEntry)
                    }
                }
            }
        }

        fun updateWidgetIntent(context: Context, application: Application): Intent {
            val intent = Intent(context, SimpleBudgetWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(application)
                    .getAppWidgetIds(ComponentName(application, SimpleBudgetWidget::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            return intent
        }
    }

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)

        res = resources
        pref = PreferenceManager.getDefaultSharedPreferences(this)
        decimalFormat = DecimalFormat(res.getString(R.string.number_format))
        DBhandler.createInstance(this)

        selectedIntervalType = pref.getInt(
                getString(R.string.selected_interval_type_key), MONTHLY_INTERVAL)

        HistoryHelper.fromDateSelection = LocalDate.now()
        HistoryHelper.toDateSelection = LocalDate.now()
    }
}