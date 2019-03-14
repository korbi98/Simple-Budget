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

package com.korbi.simplebudget.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.korbi.simplebudget.R
import com.korbi.simplebudget.logic.adapters.BudgetAdapter


class BudgetFragment : androidx.fragment.app.Fragment() {

    private lateinit var budgetRecycler: RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val rootview = inflater.inflate(R.layout.fragment_budget, container, false)

        budgetRecycler = rootview.findViewById(R.id.dashboard_budget_recycler)
        budgetRecycler.setHasFixedSize(true)
        budgetRecycler.layoutManager = LinearLayoutManager(context,
                                        RecyclerView.VERTICAL, false)
        val budgetAdapter = BudgetAdapter()
        budgetRecycler.adapter = budgetAdapter

        return rootview
    }
}