package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.KEY_SAVED_IMG_URI
import com.example.background.KEY_TEMP_BLURRED_IMG_URI
import java.io.File
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaveImageToFileWorker(context: Context, params: WorkerParameters): Worker(context, params) {

    private val title = "Blurred Image"
    private val dateFormatter = SimpleDateFormat(
        "yyyy.MM.dd 'at' HH:mm:ss z",
        Locale.getDefault()
    )

    override fun doWork(): Result {
        makeStatusNotification("Saving image", applicationContext)
        sleep()

        return try {
            val resourceUri = inputData.getString(KEY_TEMP_BLURRED_IMG_URI)
            val contentResolver = applicationContext.contentResolver
            if (resourceUri.isNullOrEmpty()) {
                Log.e(TAG, "Invalid resource uri")
                throw IllegalArgumentException("Invalid resource uri")
            }
            val file = File(resourceUri)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            val savedImageUri = MediaStore.Images.Media.insertImage(
                contentResolver, bitmap, title, dateFormatter.format(Date())
            )
            if (!savedImageUri.isNullOrEmpty()) {
                Result.success(
                    workDataOf(KEY_SAVED_IMG_URI to savedImageUri)
                )
            } else {
                Log.e(TAG, "Writing to MediaStore failed")
                Result.failure()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "SaveImageToFileWorker"
    }
}