package com.korbi.simplebudget.ui.fragments

import android.content.Intent
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.korbi.simplebudget.R
import com.korbi.simplebudget.ui.AddExpenses

class DashboardFragment : androidx.fragment.app.Fragment() {



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val fab:FloatingActionButton = rootView.findViewById(R.id.fab)
        fab.setOnClickListener {
            val addExpenseActivity = Intent(context, AddExpenses::class.java)
            startActivity(addExpenseActivity)
        }

        return rootView
    }

}
