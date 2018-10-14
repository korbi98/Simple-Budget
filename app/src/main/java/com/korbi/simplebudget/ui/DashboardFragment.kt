package com.korbi.simplebudget.ui

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.korbi.simplebudget.MainActivity

import com.korbi.simplebudget.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [DashboardFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [DashboardFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class DashboardFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootView = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val fab = rootView.findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val addExpenseActivity = Intent(context, AddExpenses::class.java)
            startActivity(addExpenseActivity)
        }

        // Inflate the layout for this fragment
        return rootView
    }

}
