package com.example.nomad

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class PostsAdapter(
    private val posts: List<ProfileActivity.Post>
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val postImage: ImageView = itemView.findViewById(R.id.postImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        Picasso.get()
            .load(post.imageUrl)
            .placeholder(R.drawable.icon_profile)
            .error(R.drawable.icon_profile)
            .fit()
            .centerCrop()
            .into(holder.postImage)
    }

    override fun getItemCount(): Int = posts.size
}