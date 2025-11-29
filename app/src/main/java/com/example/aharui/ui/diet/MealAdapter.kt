package com.example.aharui.ui.diet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aharui.R
import com.example.aharui.data.model.Meal
import com.google.android.material.chip.Chip

class MealAdapter(
    private val onMealClick: (Meal) -> Unit,
    private val onMealLongClick: ((Meal) -> Unit)? = null
) : ListAdapter<Meal, MealAdapter.MealViewHolder>(MealDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = getItem(position)
        holder.bind(meal, onMealClick, onMealLongClick)
    }

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val foodNameTextView: TextView = itemView.findViewById(R.id.food_name_textview)
        private val caloriesChip: Chip? = itemView.findViewById(R.id.calories_chip)
        private val caloriesTextView: TextView? = itemView.findViewById(R.id.calories_textview)
        private val mealTypeTextView: TextView = itemView.findViewById(R.id.meal_type_textview)
        private val proteinTextView: TextView? = itemView.findViewById(R.id.protein_textview)
        private val carbsTextView: TextView? = itemView.findViewById(R.id.carbs_textview)
        private val fatTextView: TextView? = itemView.findViewById(R.id.fat_textview)
        private val macrosTextView: TextView? = itemView.findViewById(R.id.macros_textview)

        fun bind(
            meal: Meal,
            onMealClick: (Meal) -> Unit,
            onMealLongClick: ((Meal) -> Unit)?
        ) {
            // Set food name
            foodNameTextView.text = meal.name

            // Set calories (use chip if available, otherwise textview)
            val caloriesText = "${meal.calories} kcal"
            caloriesChip?.text = caloriesText
            caloriesTextView?.text = caloriesText

            // Set individual macros if new layout is used
            if (proteinTextView != null && carbsTextView != null && fatTextView != null) {
                proteinTextView.text = "P: ${String.format("%.1f", meal.proteinG)}g"
                carbsTextView.text = "C: ${String.format("%.1f", meal.carbsG)}g"
                fatTextView.text = "F: ${String.format("%.1f", meal.fatG)}g"
            }

            // Set legacy macros textview if old layout is used
            macrosTextView?.text = "P: ${String.format("%.1f", meal.proteinG)}g | C: ${String.format("%.1f", meal.carbsG)}g | F: ${String.format("%.1f", meal.fatG)}g"

            // Set meal type with source emoji
            val mealTypeText = meal.mealType.name.lowercase()
                .replaceFirstChar { it.uppercase() }

            val sourceEmoji = when (meal.source) {
                com.example.aharui.data.model.MealSource.AI_PLAN -> " 🤖"
                com.example.aharui.data.model.MealSource.OCR -> " 📷"
                com.example.aharui.data.model.MealSource.MANUAL -> " ✏️"
            }

            mealTypeTextView.text = (mealTypeText + sourceEmoji).uppercase()

            // Handle click listeners
            itemView.setOnClickListener { onMealClick(meal) }

            onMealLongClick?.let { callback ->
                itemView.setOnLongClickListener {
                    callback(meal)
                    true
                }
            }
        }
    }

    private class MealDiffCallback : DiffUtil.ItemCallback<Meal>() {
        override fun areItemsTheSame(oldItem: Meal, newItem: Meal): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Meal, newItem: Meal): Boolean {
            return oldItem == newItem
        }
    }
}