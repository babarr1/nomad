package com.example.nomad

/**
 * Data class representing a friend/user in the friends list
 */
data class Friend(
    val userId: Int,
    val username: String,
    val fullName: String,
    val profilePictureUrl: String? = null,
    val isFriend: Boolean = false
)