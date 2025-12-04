package com.example.nomad

/**
 * Data class representing a user (can be friend or non-friend)
 */
data class User(
    val userId: Int,
    val username: String,
    val fullName: String,
    val profilePictureUrl: String? = null,
    val isFriend: Boolean = false
)