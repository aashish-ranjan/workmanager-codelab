/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.background

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.background.workers.BlurWorker
import com.example.background.workers.CleanupWorker
import com.example.background.workers.SaveImageToFileWorker


class BlurViewModel(application: Application) : ViewModel() {

    internal var imageUri: Uri? = null
    internal var outputUri: Uri? = null
    internal val saveImgToFileWorkInfos: LiveData<List<WorkInfo>>
    private val workManager = WorkManager.getInstance(application)

    init {
        imageUri = getImageUri(application.applicationContext)
        saveImgToFileWorkInfos = workManager.getWorkInfosByTagLiveData(SAVE_IMG_TO_FILE_WORK_TAG)
    }
    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     * @param blurLevel The amount to blur the image
     */
    internal fun applyBlur(blurLevel: Int) {
        val cleanupWorkRequest = OneTimeWorkRequest.from(CleanupWorker::class.java)
        val blurWorkRequest = OneTimeWorkRequestBuilder<BlurWorker>()
            .setInputData(createInputDataForUri())
            .build()

        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .build()
        val saveImageToFileWorkRequest = OneTimeWorkRequestBuilder<SaveImageToFileWorker>()
            .addTag(SAVE_IMG_TO_FILE_WORK_TAG)
            .setConstraints(constraints)
            .build()

        workManager.beginUniqueWork(CLEAR_BLUR_SAVE_FILE_CHAINED_WORK_NAME, ExistingWorkPolicy.REPLACE, cleanupWorkRequest)
            .then(blurWorkRequest)
            .then(saveImageToFileWorkRequest)
            .enqueue()
    }

    private fun uriOrNull(uriString: String?): Uri? {
        return if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            null
        }
    }

    private fun getImageUri(context: Context): Uri {
        val resources = context.resources

        val imageUri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(resources.getResourcePackageName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceTypeName(R.drawable.android_cupcake))
            .appendPath(resources.getResourceEntryName(R.drawable.android_cupcake))
            .build()

        return imageUri
    }

    internal fun setOutputUri(outputImageUri: String?) {
        outputUri = uriOrNull(outputImageUri)
    }

    private fun createInputDataForUri(): Data {
        return Data.Builder().apply {
            imageUri?.let {
                putString(ORIGINAL_IMAGE_URI, it.toString())
            }
        }.build()
    }

    fun cancelWork() {
        workManager.cancelUniqueWork(CLEAR_BLUR_SAVE_FILE_CHAINED_WORK_NAME)
    }

    class BlurViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return if (modelClass.isAssignableFrom(BlurViewModel::class.java)) {
                BlurViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
