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

package com.korbi.simplebudget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.korbi.simplebudget.ui.MainActivity
import com.korbi.simplebudget.R
import com.korbi.simplebudget.SimpleBudgetApp
import com.korbi.simplebudget.database.DBhandler
import com.korbi.simplebudget.ui.AddExpenses
import com.korbi.simplebudget.utilities.WEEKLY_INTERVAL
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import java.text.DecimalFormatSymbols

class SimpleBudgetWidget : AppWidgetProvider() {

    private val intervalType = SimpleBudgetApp.pref.getString(
            SimpleBudgetApp.res.getString(R.string.settings_key_history_grouping), "1")

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {

        for (id in appWidgetIds) {

            val width = appWidgetManager.getAppWidgetOptions(id)
                    .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

            val widgetView = RemoteViews(context.packageName, R.layout.widget_layout)

            val startApp = Intent(context, MainActivity::class.java)
            val startAppPending = PendingIntent.getActivity(context, 0, startApp, 0)
            val addExpense = Intent(context, AddExpenses::class.java)
            val addExpensesPending = PendingIntent.getActivity(context, 0, addExpense, 0)

            widgetView.setOnClickPendingIntent(R.id.widget_lin_layout, startAppPending)
            widgetView.setOnClickPendingIntent(R.id.widget_add_button, addExpensesPending)

            val intervalAmount = getAmount()
            val intervalAmountString = getAmountString(context, width)

            widgetView.setTextViewText(R.id.widget_amount_text, intervalAmountString)
            widgetView.setTextColor(R.id.widget_amount_text, when {
                intervalAmount > 0 -> ContextCompat.getColor(context, R.color.incomeColor)
                intervalAmount < 0 -> ContextCompat.getColor(context, R.color.expenseColor)
                else -> ContextCompat.getColor(context, R.color.neutralColor)
            })
            widgetView.setTextViewText(R.id.widget_interval_text, when (intervalType) {
                WEEKLY_INTERVAL.toString() -> context.getString(R.string.this_week)
                else -> context.getString(R.string.this_month)
            })

            appWidgetManager.updateAppWidget(id, widgetView)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager,
                                           appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)

        val widgetView = RemoteViews(context.packageName, R.layout.widget_layout)

        val width = appWidgetManager.getAppWidgetOptions(appWidgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

        val intervalAmountString = getAmountString(context, width)

        widgetView.setTextViewText(R.id.widget_amount_text, intervalAmountString)
        appWidgetManager.updateAppWidget(appWidgetId, widgetView)
    }

    private fun getAmountString(context: Context, width: Int): String {
        var intervalAmountString = SimpleBudgetApp.createCurrencyString(getAmount())
        if (intervalAmountString.length > 9 && width.toDp(context) < 60) {
            intervalAmountString = intervalAmountString.substringBefore(
                    DecimalFormatSymbols.getInstance().decimalSeparator) + "..."
        }
        return intervalAmountString
    }

    private fun getAmount(): Int {
        val db = DBhandler.getInstance()

        return when (intervalType) {
            WEEKLY_INTERVAL.toString() -> {
                val startOnSunday = SimpleBudgetApp.pref.getBoolean(SimpleBudgetApp.res
                        .getString(R.string.settings_key_start_week_sunday), false)

                val startDate = when (startOnSunday) {
                    false -> LocalDate.now().with ( DayOfWeek.MONDAY )
                    true -> LocalDate.now().minusWeeks(1).with ( DayOfWeek.SUNDAY )
                }
                val endDate = when (startOnSunday) {
                    false -> LocalDate.now().with ( DayOfWeek.SUNDAY )
                    true -> LocalDate.now().with ( DayOfWeek.SATURDAY )
                }
                db.getExpensesByDate(startDate, endDate).sumBy { it.cost }
            }
            else -> {
                val startDate = YearMonth.now().atDay(1)
                val endDate = YearMonth.now().atEndOfMonth()
                db.getExpensesByDate(startDate, endDate).sumBy { it.cost }
            }
        }
    }

    private fun Int.toDp(context: Context): Int {
        return (this / context.resources.displayMetrics.density).toInt()
    }
}