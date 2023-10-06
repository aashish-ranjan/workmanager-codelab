package com.example.background.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.background.OUTPUT_PATH
import java.io.File
import java.lang.Exception

class CleanupWorker(context: Context, params: WorkerParameters): Worker(context, params) {

    override fun doWork(): Result {
        makeStatusNotification("Cleaning up old temporary files", applicationContext)
        sleep()

        return try {
            val outputDirectory = File(applicationContext.filesDir, OUTPUT_PATH)
            if (outputDirectory.exists()) {
                val entries = outputDirectory.listFiles()
                entries?.forEach { entry ->
                    val name = entry.name
                    if (!name.isEmpty() && name.endsWith(".png")) {
                        val deleted = entry.delete()
                        Log.i(TAG, "Deleted $name - $deleted")
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
    companion object {
        private const val TAG = "CleanupWorker"
    }
}