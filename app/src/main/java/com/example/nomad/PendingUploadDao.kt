package com.example.nomad

import androidx.room.*

@Dao
interface PendingUploadDao {

    @Insert
    suspend fun insertUpload(upload: PendingUploadEntity)

    @Query("SELECT * FROM pending_uploads")
    suspend fun getAllPendingUploads(): List<PendingUploadEntity>

    @Query("DELETE FROM pending_uploads WHERE id = :uploadId")
    suspend fun deleteUpload(uploadId: Int)
}
