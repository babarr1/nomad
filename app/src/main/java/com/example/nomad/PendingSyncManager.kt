package com.example.nomad

import android.content.Context
import android.net.Uri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.File

object PendingSyncManager {

    fun syncPendingUploads(context: Context) {

        // ✅ If no internet, do nothing
        if (!isInternetAvailable(context)) return

        val db = AppDatabase.getDatabase(context)
        val dao = db.pendingUploadDao()
        val uploadManager = UploadManager(context)
        val authManager = AuthManager(context)

        val userId = authManager.getUserId()
        if (userId == -1) return

        CoroutineScope(Dispatchers.Main).launch {

            val pendingUploads = dao.getAllPendingUploads()

            for (post in pendingUploads) {

                val imageFile = File(post.imagePath)

                if (imageFile.exists()) {

                    val imageUri = Uri.fromFile(imageFile)

                    val result = uploadManager.uploadDiscovery(
                        userId = userId,
                        categoryId = post.categoryId,
                        caption = post.caption,
                        imageUri = imageUri
                    )

                    // ✅ If upload success → delete from queue
                    if (result is UploadManager.UploadResult.Success) {
                        dao.deleteUpload(post.id)
                    }
                }
            }
        }
    }
}
