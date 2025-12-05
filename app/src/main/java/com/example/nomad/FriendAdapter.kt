package com.example.nomad

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

/**
 * Adapter for displaying all users in RecyclerView with circular profile images
 */
class FriendAdapter(
    private var users: List<Friend>,
    private val onActionClick: (Friend) -> Unit
) : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.friendProfileImage)
        val nameText: TextView = itemView.findViewById(R.id.friendNameText)
        val usernameText: TextView = itemView.findViewById(R.id.friendUsernameText)
        val actionButton: ImageView = itemView.findViewById(R.id.friendActionButton)
        val friendBadge: TextView = itemView.findViewById(R.id.friendBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val user = users[position]

        // Set user info
        holder.nameText.text = user.fullName
        holder.usernameText.text = "@${user.username}"

        // Show friend badge or add button based on friendship status
        if (user.isFriend) {
            holder.friendBadge.visibility = View.VISIBLE
            holder.actionButton.visibility = View.GONE
        } else {
            holder.friendBadge.visibility = View.GONE
            holder.actionButton.visibility = View.VISIBLE
        }

        // Load profile image using Picasso with circular transformation
        if (!user.profilePictureUrl.isNullOrEmpty()) {
            Picasso.get()
                .load(user.profilePictureUrl)
                .transform(CircleTransform()) // Apply circular transformation
                .placeholder(R.drawable.icon_profile)
                .error(R.drawable.icon_profile)
                .fit()
                .centerCrop()
                .into(holder.profileImage)
        } else {
            // Use default profile icon if no picture URL
            holder.profileImage.setImageResource(R.drawable.icon_profile)
        }

        // Handle action button click (add friend or view profile)
        holder.actionButton.setOnClickListener {
            onActionClick(user)
        }

        // Make entire item clickable to view profile
        holder.itemView.setOnClickListener {
            onActionClick(user)
        }
    }

    override fun getItemCount(): Int = users.size

    /**
     * Update the list of users
     */
    fun updateUsers(newUsers: List<Friend>) {
        users = newUsers
        notifyDataSetChanged()
    }
}