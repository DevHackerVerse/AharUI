package com.example.aharui.ui.profile

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.aharui.R
import com.example.aharui.data.model.Badge

class BadgeAdapter : ListAdapter<Badge, BadgeAdapter.BadgeViewHolder>(BadgeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BadgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_badge, parent, false)
        return BadgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: BadgeViewHolder, position: Int) {
        val badge = getItem(position)
        holder.bind(badge)
    }

    class BadgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val badgeImageView: ImageView = itemView.findViewById(R.id.badge_imageview)
        private val badgeNameTextView: TextView = itemView.findViewById(R.id.badge_name_textview)
        private val badgeDescriptionTextView: TextView = itemView.findViewById(R.id.badge_description_textview)

        fun bind(badge: Badge) {
            badgeNameTextView.text = badge.name
            badgeDescriptionTextView.text = badge.description

            if (badge.isUnlocked) {
                badgeImageView.alpha = 1.0f
                badgeNameTextView.setTextColor(Color.BLACK)
            } else {
                badgeImageView.alpha = 0.3f
                badgeNameTextView.setTextColor(Color.GRAY)
            }
        }
    }

    private class BadgeDiffCallback : DiffUtil.ItemCallback<Badge>() {
        override fun areItemsTheSame(oldItem: Badge, newItem: Badge): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Badge, newItem: Badge): Boolean {
            return oldItem == newItem
        }
    }
}