package com.korbi.simplebudget.logic.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.korbi.simplebudget.R
import kotlinx.android.synthetic.main.category_listening.view.*



class CategoryAdapter(private val categories: MutableList<String>) :
                androidx.recyclerview.widget.RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var selectedCategory: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val categoryView = LayoutInflater.from(parent.context).inflate(R.layout.category_listening,
                parent, false)

        return ViewHolder(categoryView)
    }

    override fun onBindViewHolder(parent: ViewHolder, position: Int) {

        parent.categoryNameView.text = categories[position]
        parent.categoryIconView.setBackgroundResource(R.drawable.ic_add_white_24dp)

        if (selectedCategory == categories[position]){
            parent.itemView.setBackgroundResource(R.color.colorPrimaryDark)
        } else {
            parent.itemView.setBackgroundResource(android.R.color.transparent)
        }
    }

    override fun getItemCount(): Int {
        return categories.size
    }

    inner class ViewHolder(categoryView: View) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(categoryView) {

        val categoryNameView: TextView = categoryView.category_listening_name
        val categoryIconView: ImageView = categoryView.category_listening_icon

        init {
            categoryView.setOnClickListener {
                if (selectedCategory == categories[adapterPosition]) {
                    selectedCategory = null
                    notifyDataSetChanged()
                } else {
                    selectedCategory = categories[adapterPosition]
                    notifyDataSetChanged()
                }
            }
        }
    }

    fun getSelectedCategory(): String? {
        return selectedCategory
    }

    fun setSelectedCategory(category: String) {
        selectedCategory = category
        notifyItemChanged(categories.indexOf(category))
    }
}