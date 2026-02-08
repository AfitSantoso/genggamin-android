package com.example.genggaminmobile.domain.util

import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * Result wrapper for Cloudinary upload operations
 */
sealed class CloudinaryUploadResult {
    data class Success(val secureUrl: String, val publicId: String) : CloudinaryUploadResult()
    data class Error(val message: String) : CloudinaryUploadResult()
}

/**
 * Utility object for uploading files to Cloudinary
 */
object CloudinaryUploader {

    private const val TAG = "CloudinaryUploader"
    private const val UPLOAD_PRESET = "genggamin_preset"

    /**
     * Upload a PDF file to Cloudinary using unsigned upload
     * @param file The PDF file to upload
     * @return CloudinaryUploadResult with secure URL on success or error message on failure
     */
    suspend fun uploadPdf(file: File): CloudinaryUploadResult {
        return suspendCancellableCoroutine { continuation ->
            try {
                val requestId = MediaManager.get()
                    .upload(file.absolutePath)
                    .unsigned(UPLOAD_PRESET)
                    .option("resource_type", "raw") // Required for non-image files like PDF
                    .option("folder", "loan_contracts") // Organize in folder
                    .option("tags", "contract,loan,genggamin") // Add tags for easy search
                    .callback(object : UploadCallback {
                        override fun onStart(requestId: String) {
                            Log.d(TAG, "Upload started: $requestId")
                        }

                        override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                            val progress = if (totalBytes > 0) (bytes * 100 / totalBytes) else 0
                            Log.d(TAG, "Upload progress: $progress%")
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val secureUrl = resultData["secure_url"] as? String
                            val publicId = resultData["public_id"] as? String

                            Log.d(TAG, "Upload success! URL: $secureUrl")

                            if (secureUrl != null && publicId != null) {
                                if (continuation.isActive) {
                                    continuation.resume(CloudinaryUploadResult.Success(secureUrl, publicId))
                                }
                            } else {
                                if (continuation.isActive) {
                                    continuation.resume(CloudinaryUploadResult.Error("Invalid response from Cloudinary"))
                                }
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            Log.e(TAG, "Upload error: ${error.description}")
                            if (continuation.isActive) {
                                continuation.resume(CloudinaryUploadResult.Error(error.description ?: "Unknown upload error"))
                            }
                        }

                        override fun onReschedule(requestId: String, error: ErrorInfo) {
                            Log.w(TAG, "Upload rescheduled: ${error.description}")
                        }
                    })
                    .dispatch()

                // Handle cancellation
                continuation.invokeOnCancellation {
                    try {
                        MediaManager.get().cancelRequest(requestId)
                        Log.d(TAG, "Upload cancelled: $requestId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cancelling upload: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload exception: ${e.message}")
                if (continuation.isActive) {
                    continuation.resume(CloudinaryUploadResult.Error(e.message ?: "Upload failed"))
                }
            }
        }
    }

    /**
     * Upload PDF file with callback (non-suspending version)
     */
    fun uploadPdfWithCallback(
        filePath: String,
        onSuccess: (secureUrl: String) -> Unit,
        onError: (message: String) -> Unit,
    ) {
        try {
            MediaManager.get()
                .upload(filePath)
                .unsigned(UPLOAD_PRESET)
                .option("resource_type", "raw")
                .option("folder", "loan_contracts")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d(TAG, "Callback upload started")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val secureUrl = resultData["secure_url"] as? String
                        if (secureUrl != null) {
                            onSuccess(secureUrl)
                        } else {
                            onError("Failed to get upload URL")
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        onError(error.description ?: "Upload failed")
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch()
        } catch (e: Exception) {
            onError(e.message ?: "Upload exception")
        }
    }
}
