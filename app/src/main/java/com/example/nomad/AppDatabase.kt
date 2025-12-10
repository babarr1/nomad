package com.example.nomad

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OfflinePostEntity::class,
        PendingUploadEntity::class   // ✅ ADDED FOR OFFLINE UPLOAD QUEUE
    ],
    version = 2                      // ✅ VERSION UPDATED
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun offlinePostDao(): OfflinePostDao

    // ✅ ADDED DAO FOR PENDING UPLOADS
    abstract fun pendingUploadDao(): PendingUploadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nomad_offline_db"
                )
                    .fallbackToDestructiveMigration() // ✅ IMPORTANT to avoid crash
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
