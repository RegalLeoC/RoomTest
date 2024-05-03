package com.example.roomtest

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.roomtest.R // Make sure to import your R file correctly
import com.example.roomtest.dataclass.User

class UserSelectionViewHolder(itemView: View, private val onItemClick: (User) -> Unit) : RecyclerView.ViewHolder(itemView) {
    val textViewUserName: TextView = itemView.findViewById(R.id.textViewUserName)


    fun bind(user: User) {
        textViewUserName.text = user.name
        itemView.setOnClickListener { onItemClick(user) }
    }

}