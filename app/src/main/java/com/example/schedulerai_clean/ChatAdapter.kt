package com.example.schedulerai_clean

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: List<Message>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUser: TextView = view.findViewById(R.id.tvUserMessage)
        val tvAi: TextView = view.findViewById(R.id.tvAiMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        if (message.isUser) {
            holder.tvUser.text = message.text
            holder.tvUser.visibility = View.VISIBLE
            holder.tvAi.visibility = View.GONE
        } else {
            holder.tvAi.text = message.text
            holder.tvAi.visibility = View.VISIBLE
            holder.tvUser.visibility = View.GONE
        }
    }

    override fun getItemCount() = messages.size
}