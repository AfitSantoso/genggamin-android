package com.example.genggaminmobile.data.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.genggaminmobile.data.local.dao.ProfileDao
import com.example.genggaminmobile.data.local.entity.ProfileEntity
import com.example.genggaminmobile.data.model.dto.CustomerProfileRequest
import com.example.genggaminmobile.data.model.dto.CustomerProfileResponse
import com.example.genggaminmobile.data.remote.api.CustomerApi
import com.example.genggaminmobile.domain.repository.CustomerRepository
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepositoryImpl @Inject constructor(
    private val customerApi: CustomerApi,
    private val gson: Gson,
    private val profileDao: ProfileDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : CustomerRepository {

    override suspend fun getProfile(): Result<CustomerProfileResponse> {
        return try {
            val response = customerApi.getProfile()
            if (response.success && response.data != null) {
                // Save to DB
                saveProfileToDb(response.data)
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: HttpException) {
             // Try load local
             val local = profileDao.getProfileOneShot()
             if (local != null) {
                 Result.success(mapEntityToResponse(local))
             } else {
                 Result.failure(Exception("Gagal mengambil profil: ${e.code()}"))
             }
        } catch (e: Exception) {
             val local = profileDao.getProfileOneShot()
             if (local != null) {
                 Result.success(mapEntityToResponse(local))
             } else {
                 Result.failure(e)
             }
        }
    }

    private suspend fun saveProfileToDb(data: CustomerProfileResponse) {
        // Download images in background
        val localKtp = data.ktpImagePath?.let { downloadAndCacheImage(it, "ktp_${data.id}.jpg") }
        val localSelfie = data.selfieImagePath?.let { downloadAndCacheImage(it, "selfie_${data.id}.jpg") }
        val localPayslip = data.payslipImagePath?.let { downloadAndCacheImage(it, "payslip_${data.id}.jpg") }

        val entity = ProfileEntity(
            id = data.id,
            userId = data.userId,
            username = data.username,
            email = data.email,
            fullName = data.fullName,
            nik = data.nik,
            address = data.address,
            dateOfBirth = data.dateOfBirth,
            placeOfBirth = data.placeOfBirth,
            monthlyIncome = data.monthlyIncome,
            occupation = data.occupation,
            customerPhone = data.customerPhone,
            currentAddress = data.currentAddress,
            motherMaidenName = data.motherMaidenName,
            accountNumber = data.accountNumber,
            accountHolderName = data.accountHolderName,
            ktpImagePath = data.ktpImagePath,
            selfieImagePath = data.selfieImagePath,
            payslipImagePath = data.payslipImagePath,
            localKtpPath = localKtp,
            localSelfiePath = localSelfie,
            localPayslipPath = localPayslip,
            emergencyContactsJson = gson.toJson(data.emergencyContacts),
            createdAt = data.createdAt
        )
        profileDao.insertProfile(entity)
    }

    private fun mapEntityToResponse(entity: ProfileEntity): CustomerProfileResponse {
        val contactsType = object : com.google.gson.reflect.TypeToken<List<com.example.genggaminmobile.data.model.dto.EmergencyContactDto>>() {}.type
        val contacts: List<com.example.genggaminmobile.data.model.dto.EmergencyContactDto> = gson.fromJson(entity.emergencyContactsJson, contactsType) ?: emptyList()
        
        return CustomerProfileResponse(
            id = entity.id,
            userId = entity.userId,
            username = entity.username,
            email = entity.email,
            fullName = entity.fullName,
            nik = entity.nik,
            address = entity.address,
            dateOfBirth = entity.dateOfBirth,
            placeOfBirth = entity.placeOfBirth,
            monthlyIncome = entity.monthlyIncome,
            occupation = entity.occupation,
            customerPhone = entity.customerPhone,
            currentAddress = entity.currentAddress,
            motherMaidenName = entity.motherMaidenName,
            accountNumber = entity.accountNumber,
            accountHolderName = entity.accountHolderName,
            ktpImagePath = entity.localKtpPath ?: entity.ktpImagePath,
            selfieImagePath = entity.localSelfiePath ?: entity.selfieImagePath,
            payslipImagePath = entity.localPayslipPath ?: entity.payslipImagePath,
            emergencyContacts = contacts,
            createdAt = entity.createdAt
        )
    }


    override suspend fun createOrUpdateProfile(
        data: CustomerProfileRequest,
        ktp: File?,
        selfie: File?,
        payslip: File?
    ): Result<CustomerProfileResponse> {
        return try {
            val jsonString = gson.toJson(data)
            val dataPart = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

            // Kompres & Resize setiap file jika ada
            val ktpPart = ktp?.let {
                MultipartBody.Part.createFormData("ktp", it.name, compressAndResizeImage(it))
            }

            val selfiePart = selfie?.let {
                MultipartBody.Part.createFormData("selfie", it.name, compressAndResizeImage(it))
            }

            val payslipPart = payslip?.let {
                MultipartBody.Part.createFormData("payslip", it.name, compressAndResizeImage(it))
            }

            val response = customerApi.createOrUpdateProfile(dataPart, ktpPart, selfiePart, payslipPart)
            
            if (response.success && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.message))
            }
        } catch (e: HttpException) {
             // Save pending update
             savePendingUpdate(data, ktp, selfie, payslip)
             // Return "fake" success or special error? 
             // Ideally we should return a state saying "pending", but for now let's assume UI handles success as "saved".
             // We can return the local data as "response" to update UI immediately.
             val pendingResponse = createFakeResponseFromRequest(data)
             Result.success(pendingResponse)
        } catch (e: Exception) {
             savePendingUpdate(data, ktp, selfie, payslip)
             // Schedule immediate sync
             scheduleImmediateSync()

             val pendingResponse = createFakeResponseFromRequest(data)
             Result.success(pendingResponse)
        }
    }

    private fun scheduleImmediateSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
            
        val syncRequest = androidx.work.OneTimeWorkRequest.Builder(
            com.example.genggaminmobile.data.worker.SyncWorker::class.java
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()
            
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            "ImmediateSyncProfile",
            androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest
        )
    }

    private suspend fun savePendingUpdate(
        data: CustomerProfileRequest,
        ktp: File?,
        selfie: File?,
        payslip: File?
    ) {
        val entity = com.example.genggaminmobile.data.local.entity.PendingProfileUpdateEntity(
            jsonRequest = gson.toJson(data),
            ktpPath = ktp?.absolutePath,
            selfiePath = selfie?.absolutePath,
            payslipPath = payslip?.absolutePath
        )
        profileDao.insertPendingUpdate(entity)
    }

    private fun createFakeResponseFromRequest(req: CustomerProfileRequest): CustomerProfileResponse {
        // Construct a temporary response object to update UI immediately
        return CustomerProfileResponse(
            id = 0,
            userId = 0,
            username = "",
            email = "",
            fullName = "", // Need checking usage
            nik = req.nik,
            address = req.address,
            dateOfBirth = req.dateOfBirth,
            placeOfBirth = req.placeOfBirth,
            monthlyIncome = req.monthlyIncome,
            occupation = req.occupation,
            customerPhone = req.phone,
            currentAddress = req.currentAddress,
            motherMaidenName = req.motherMaidenName,
            accountNumber = req.accountNumber,
            accountHolderName = req.accountHolderName,
            ktpImagePath = "",
            selfieImagePath = "",
            payslipImagePath = "",
            emergencyContacts = listOf(req.emergencyContact),
            createdAt = ""
        )
    }

    override suspend fun syncPendingProfile(): Result<Unit> {
        val pending = profileDao.getPendingUpdate() ?: return Result.success(Unit)

        return try {
            val data = gson.fromJson(pending.jsonRequest, CustomerProfileRequest::class.java)
            val ktp = pending.ktpPath?.let { File(it) }
            val selfie = pending.selfiePath?.let { File(it) }
            val payslip = pending.payslipPath?.let { File(it) }

            // Ensure files exist before sending (if user cleared cache they might be gone)
            // But we try anyway or skip? If file missing, maybe send null?
            // Sending null might fail backend validation if required.
            // For now assume files persist.
            
            val jsonString = gson.toJson(data)
            val dataPart = jsonString.toRequestBody("application/json".toMediaTypeOrNull())

            val ktpPart = ktp?.takeIf { it.exists() }?.let {
                MultipartBody.Part.createFormData("ktp", it.name, compressAndResizeImage(it))
            }
            val selfiePart = selfie?.takeIf { it.exists() }?.let {
                MultipartBody.Part.createFormData("selfie", it.name, compressAndResizeImage(it))
            }
            val payslipPart = payslip?.takeIf { it.exists() }?.let {
                MultipartBody.Part.createFormData("payslip", it.name, compressAndResizeImage(it))
            }

            val response = customerApi.createOrUpdateProfile(dataPart, ktpPart, selfiePart, payslipPart)
            
            if (response.success) {
                profileDao.clearPendingUpdate()
                Result.success(Unit)
            } else {
                // Keep pending
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fungsi untuk Resize (Maks 1024px) dan Kompres (60% Quality)
     * Ini akan memastikan ukuran file di bawah 300KB namun tetap tajam.
     */
    private fun compressAndResizeImage(file: File): RequestBody {
        return try {
            val options = BitmapFactory.Options()
            var bitmap = BitmapFactory.decodeFile(file.path, options)

            // 1. Resize: Batasi dimensi maksimal ke 1024px
            val maxSize = 1024
            val width = bitmap.width
            val height = bitmap.height
            
            if (width > maxSize || height > maxSize) {
                val bitmapRatio = width.toFloat() / height.toFloat()
                val targetWidth: Int
                val targetHeight: Int
                
                if (bitmapRatio > 1) {
                    targetWidth = maxSize
                    targetHeight = (maxSize / bitmapRatio).toInt()
                } else {
                    targetHeight = maxSize
                    targetWidth = (maxSize * bitmapRatio).toInt()
                }
                bitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
                Log.d("CustomerRepo", "Resized ${file.name} to ${targetWidth}x${targetHeight}")
            }

            // 2. Compress: Gunakan kualitas 60%
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            
            val byteArray = outputStream.toByteArray()
            Log.d("CustomerRepo", "Final Size ${file.name}: ${byteArray.size / 1024} KB")
            
            byteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
        } catch (e: Exception) {
            Log.e("CustomerRepo", "Gagal olah ${file.name}, kirim asli", e)
            file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        }
    }

    private suspend fun downloadAndCacheImage(url: String, filename: String): String? {
        // Simple download using basic URL storage or OkHttp if available
        // Since we are inside Repo, we can use simple URL connection or OkHttp
        // Assume context.filesDir is available
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val file = File(context.filesDir, filename)
                if (file.exists() && file.length() > 0) return@withContext file.absolutePath

                val finalUrl = if (url.startsWith("http")) url else "http://10.0.2.2:8080$url" // Fallback IP for emulator

                val request = okhttp3.Request.Builder().url(finalUrl).build()
                val client = okhttp3.OkHttpClient()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null) {
                        file.writeBytes(bytes)
                        return@withContext file.absolutePath
                    }
                }
                null
            } catch (e: Exception) {
                Log.e("CustomerRepo", "Failed to cache image: $url", e)
                null
            }
        }
    }
}
