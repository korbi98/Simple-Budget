package com.korbi.simplebudget.logic

import android.animation.ObjectAnimator
import android.content.Context
import android.widget.TextView
import com.bignerdranch.expandablerecyclerview.ParentViewHolder
import com.korbi.simplebudget.R
import android.view.View
import android.widget.ImageView
import android.widget.Toast


class HistoryViewHolder(private val historyEntryView: View) : ParentViewHolder<HistoryEntry,
                                                                        Expense>(historyEntryView) {

    val context: Context = historyEntryView.context

    private val entryDate = historyEntryView.findViewById<TextView>(R.id.history_date_listing_date)
    private val expandArrow = historyEntryView.findViewById<ImageView>(R.id.history_date_listing_arrow_expand)

    override fun collapseView() {
        super.collapseView()
        val rotate = ObjectAnimator.ofFloat(expandArrow, "rotation", 90f, 0f)
        rotate.duration = 200
        rotate.start()
    }

    override fun expandView() {
        super.expandView()
        val rotate = ObjectAnimator.ofFloat(expandArrow, "rotation", 0f, 90f)
        rotate.duration = 200
        rotate.start()
    }

    fun bind (week: String) {
        entryDate.text = week

        if (isExpanded) {
            val rotate = ObjectAnimator.ofFloat(expandArrow, "rotation", 0f, 90f)
            rotate.duration = 0
            rotate.start()
        }

        historyEntryView.setOnClickListener {

            if (isExpanded) {
                collapseView()
            } else {
                expandView()
            }
        }
    }
}


