package com.korbi.simplebudget.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.korbi.simplebudget.R

class ManageCategories : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_categories)
        setTitle(R.string.manage_categories_titel)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
