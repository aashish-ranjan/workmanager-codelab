package com.example.background.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.background.ORIGINAL_IMAGE_URI
import com.example.background.KEY_TEMP_BLURRED_IMG_URI
import java.lang.Exception
import java.lang.IllegalArgumentException

class BlurWorker(context: Context, params: WorkerParameters): Worker(context, params) {

    companion object {
        const val TAG = "BlurWorker"
    }

    override fun doWork(): Result {
        val appContext = applicationContext
        makeStatusNotification("Blurring image", appContext)
        sleep()
        return try {
            val inputImageUri = inputData.getString(ORIGINAL_IMAGE_URI)
            if (inputImageUri.isNullOrEmpty()) {
                Log.e(TAG, "Invalid input uri")
                throw IllegalArgumentException("Invalid input uri")
            }
            val contentResolver = appContext.contentResolver
            val inputBitmap = BitmapFactory.decodeStream(
                contentResolver.openInputStream(Uri.parse(inputImageUri))
            )
            val blurredBitmap = blurBitmap(inputBitmap, appContext)
            val blurredFileUri = writeBitmapToFile(appContext, blurredBitmap).path
            makeStatusNotification("Blurring complete", appContext)
            Result.success(
                workDataOf(
                    KEY_TEMP_BLURRED_IMG_URI to blurredFileUri
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error applying blur")
            Result.failure()
        }
    }
}