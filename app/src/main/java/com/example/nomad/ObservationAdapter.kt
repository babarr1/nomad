package com.example.nomad

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.nomad.ObservationManager.ObservationItem
import com.squareup.picasso.MemoryPolicy
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat // NEW IMPORT
import java.util.Date             // NEW IMPORT
import java.util.Locale           // NEW IMPORT
import java.util.concurrent.TimeUnit // NEW IMPORT
import android.widget.PopupMenu
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun showPopupMenu(view: View, item: ObservationManager.ObservationItem) {
    val context = view.context // Get the Context from the view
    val authManager = AuthManager(context)

    // CRITICAL SECURITY CHECK: Only show the menu if the logged-in user owns the post.
    if (authManager.getUserId() != item.userId) {
        return
    }

    val popup = PopupMenu(context, view)
    // Assumes R.menu.post_menu exists and contains action_edit and action_delete
    popup.menuInflater.inflate(R.menu.post_menu, popup.menu)

    popup.setOnMenuItemClickListener { menuItem ->
        when (menuItem.itemId) {
            R.id.action_edit -> {
                // Launch EditActivity

                // IMPORTANT: We check if the Context is an Activity before starting
                if (context is Activity) {
                    val intent = Intent(context, EditActivity::class.java)
                    intent.putExtra("OBSERVATION_ID", item.id) // Pass the post ID
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "Error: Cannot launch edit screen.", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.action_delete -> {
                // Execute the delete operation (Assumes deletePost is defined elsewhere)
                deletePost(context, item)
                true
            }
            else -> false
        }
    }
    popup.show()
}

private fun Nothing?.startActivity(intent: Intent) {}

private fun deletePost(context: Context, item: ObservationManager.ObservationItem) {
    // TODO: Create a separate API manager (e.g., PostActionManager) or put this in ObservationManager
    // For now, we'll assume the delete function is accessible.

    Toast.makeText(context, "Deleting post...", Toast.LENGTH_SHORT).show()

    CoroutineScope(Dispatchers.Main).launch {
        // You must implement this function in a manager class
        // It requires the post ID and the user ID for security check
        val success = PostActionManager(context).deleteObservation(item.id, item.userId)

        withContext(Dispatchers.Main) {
            if (success) {
                // Success: Tell the user and refresh the list
                Toast.makeText(context, "Post deleted!", Toast.LENGTH_SHORT).show()
                // NOTE: To properly remove the item, you need a callback to the Activity
                // to update the adapter list, but for now, we'll prompt a full refresh.

                // OPTIONAL: Simple method to refresh the *current* category list:
                // If you are using the onResume() refresh, a quick restart of the activity works:
                val intent = (context as Activity).intent
                context.finish()
                context.startActivity(intent)

            } else {
                Toast.makeText(context, "Deletion failed. Server error.", Toast.LENGTH_LONG).show()
            }
        }
    }
}
    /**
     * Adapter used by all category activities (Fauna, Verdant, Strata, Phenomena)
     * to display the complex social Post Card layout.
     */
    class ObservationAdapter(
        private var observationList: List<ObservationItem>
    ) : RecyclerView.Adapter<ObservationAdapter.ObservationViewHolder>() {

        inner class ObservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            // 1. User Info Header
            val userImageLayout: ImageView = itemView.findViewById(R.id.postUserImage)
            val usernameText: TextView = itemView.findViewById(R.id.postUsername)
            val timestampText: TextView = itemView.findViewById(R.id.postTimestamp)

            // 2. Post Content
            val postImage: ImageView = itemView.findViewById(R.id.postImage)
            val postCaption: TextView = itemView.findViewById(R.id.postCaption)

            // 3. Action Buttons (Counts)
            val likeCount: TextView = itemView.findViewById(R.id.likeCount)

            val postMenuBtn: ImageView = itemView.findViewById(R.id.postMenuBtn)

            // Action ImageViews (Clickable) - Like, Comment, Menu, Share
            val likeBtn: ImageView = itemView.findViewById(R.id.likeBtn)

        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ObservationViewHolder {
            // NOTE: Assume this layout file is named 'item_observation.xml' or 'item_post.xml'
            // Since you didn't provide the name, we'll assume R.layout.item_post for this structure
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.postcarditem, parent, false)
            return ObservationViewHolder(view)
        }

        override fun onBindViewHolder(holder: ObservationViewHolder, position: Int) {
            val item = observationList[position]

            // --- Data Binding ---

            val context = holder.itemView.context
            val authManager = AuthManager(context)
            val userId = authManager.getUserId()
            val actionManager = PostActionManager(context)

            // 1. Post Content
            holder.postCaption.text = item.title // Using title/caption from ObservationItem

            // 2. Image Loading
            if (item.imageUrl.isNotEmpty()) {
                Picasso.get()
                    .load(item.imageUrl)
                    .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                    .networkPolicy(NetworkPolicy.NO_CACHE)
                    .placeholder(R.drawable.icon_fauna)
                    .error(R.drawable.icon_fauna)
                    .fit()
                    .centerCrop()
                    .into(holder.postImage)
            }

            holder.postMenuBtn.setOnClickListener { view ->
                showPopupMenu(view, item)
            }
            // 3. User Info & Timestamp (Needs more work, but we bind what we have)
            // NOTE: To get the actual username/user image, you would need a separate API call
            // using item.userId, or the server must join the users table and include user data
            // in the observation API response. For now, we set placeholders.
            holder.usernameText.text = "User #${item.userId}"
            holder.timestampText.text = getRelativeTime(item.dateObserved)

            holder.postCaption.setOnClickListener { view ->
                val currentState = view.tag as String?

                if (currentState == "COLLAPSED") {
                    // Expand
                    holder.postCaption.maxLines = Int.MAX_VALUE
                    view.tag = "EXPANDED"
                } else {
                    // Collapse
                    holder.postCaption.maxLines = 2
                    view.tag = "COLLAPSED"
                }

                // !!! CRITICAL LINE: Consume the event so the parent CardView doesn't receive it !!!
                true
            }


            // 4. Counts (Assuming 0 since the API doesn't yet provide like/comment counts)
            holder.likeCount.text = "0"


            // --- Click Listeners for Social Features ---
            holder.likeBtn.setOnClickListener {


                val userId = AuthManager(context).getUserId()

                if (userId <= 0) {
                    Toast.makeText(context, "Please log in to like posts.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                CoroutineScope(Dispatchers.Main).launch {
                    // Optimistic update: Assume success and change icon immediately for better UX
                    // This requires your ObservationItem to have an 'isLiked' field, but we skip that for now.

                    val success = actionManager.toggleLike(item.id, userId)

                    if (success) {
                        // NOTE: A full screen refresh (re-loading all data) is the most robust way
                        // to update the icon and count after the toggle.

                        Toast.makeText(context, "Like status toggled.", Toast.LENGTH_SHORT).show()

                        // To see the change, you need to trigger a data refresh on the current activity:
                        if (context is Activity) {
                            val intent = context.intent
                            context.finish()
                            context.startActivity(intent)
                        }

                    } else {
                        Toast.makeText(context, "Failed to toggle like. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // TODO: Implement Comment/Menu/Share button clicks
        }


        override fun getItemCount(): Int = observationList.size

        fun updateData(newObservationList: List<ObservationItem>) {
            observationList = newObservationList
            notifyDataSetChanged()
        }
        // --- HELPER FUNCTION FOR RELATIVE TIME FORMATTING ---

        /**
         * Converts a database ISO timestamp string (YYYY-MM-DD HH:MM:SS)
         * to a relative time string (e.g., "5m ago", "3h ago", "2w ago").
         */
        private fun getRelativeTime(dateString: String?): String {
            dateString ?: return "N/A"

            // Database time format
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            try {
                val date = format.parse(dateString) ?: return dateString
                val now = Date()
                val diff = now.time - date.time

                // Calculate differences in various time units
                val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                val hours = TimeUnit.MILLISECONDS.toHours(diff)
                val days = TimeUnit.MILLISECONDS.toDays(diff)

                return when {
                    seconds < 60 -> "${seconds}s ago"
                    minutes < 60 -> "${minutes}m ago"
                    hours < 24 -> "${hours}h ago"
                    days < 7 -> "${days}d ago"
                    days < 30 -> "${days / 7}w ago"
                    days < 365 -> "${days / 30}mo ago"
                    else -> "${days / 365}y ago"
                }

            } catch (e: Exception) {
                // Log error if date parsing fails, but return raw string as fallback
                Log.e("TimeFormatter", "Date parsing failed: ${e.message}")
                return dateString
            }
        }
    }

