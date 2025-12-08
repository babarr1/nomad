package com.example.nomad

import androidx.room.*

@Dao
interface OfflinePostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<OfflinePostEntity>)

    @Query("SELECT * FROM offline_posts WHERE category = :category")
    suspend fun getPostsByCategory(category: String): List<OfflinePostEntity>

    @Query("DELETE FROM offline_posts")
    suspend fun clearAll()
}
